package dev.breda.tap

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

class PreferenceSessionStore(context: Context) : SessionStore {
    private val preferences = context.getSharedPreferences("portfolio-account", 0)
    override var onboarded: Boolean
        get() = preferences.getBoolean("onboarded", false)
        set(value) {
            preferences.edit().putBoolean("onboarded", value).apply()
        }

    override fun select(uid: String) {
        check(preferences.edit().putString("namespace", sessionNamespace(uid)).commit()) {
            "Não foi possível abrir esta conta."
        }
    }
}

/** Adapter: Firebase exceptions and models never cross the authentication port. */
class FirebaseAuthentication(context: Context) : Authentication {
    private val auth: FirebaseAuth? =
        try {
            val configuration =
                JSONObject(
                    context.assets.open("firebase-config.json").bufferedReader().use {
                        it.readText()
                    }
                )
            val app =
                FirebaseApp.getApps(context).firstOrNull { it.name == "tap-demo" }
                    ?: FirebaseApp.initializeApp(
                        context,
                        FirebaseOptions.Builder()
                            .setProjectId(configuration.getString("projectId"))
                            .setApplicationId(configuration.getString("applicationId"))
                            .setApiKey(configuration.getString("apiKey"))
                            .build(),
                        "tap-demo",
                    )
            FirebaseAuth.getInstance(app).also { it.setLanguageCode("pt") }
        } catch (_: Exception) {
            null
        }
    override val configured
        get() = auth != null

    override fun observe(onChange: (Account?) -> Unit): AutoCloseable {
        val listener =
            FirebaseAuth.AuthStateListener { value ->
                onChange(
                    value.currentUser
                        ?.takeUnless { it.isAnonymous }
                        ?.let {
                            Account(it.uid, it.email.orEmpty(), it.displayName ?: "Minha conta")
                        }
                )
            }
        auth?.addAuthStateListener(listener)
        return AutoCloseable { auth?.removeAuthStateListener(listener) }
    }

    override suspend fun login(email: String, password: String) = request {
        AccountValidation.login(email, password)
        client().signInWithEmailAndPassword(email.trim(), password).await()
        Unit
    }

    override suspend fun register(name: String, email: String, password: String) = request {
        AccountValidation.register(name, email, password)
        val result = client().createUserWithEmailAndPassword(email.trim(), password).await()
        result.user
            ?.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name.trim()).build())
            ?.await()
        Unit
    }

    override suspend fun reset(email: String) = request {
        AccountValidation.email(email)
        client().sendPasswordResetEmail(email.trim()).await()
        Unit
    }

    override suspend fun verify() = request {
        requireNotNull(client().currentUser).sendEmailVerification().await()
        Unit
    }

    override fun signOut() {
        auth?.signOut()
    }

    private fun client() = requireNotNull(auth) { "Configure o Firebase para entrar." }

    private suspend fun request(block: suspend () -> Unit) {
        try {
            block()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            val message =
                when (failure) {
                    is com.google.firebase.FirebaseNetworkException ->
                        "Sem conexão. Confira sua rede e tente novamente."
                    is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException,
                    is com.google.firebase.auth.FirebaseAuthInvalidUserException ->
                        "Não foi possível autenticar. Confira e-mail e senha."
                    is com.google.firebase.auth.FirebaseAuthUserCollisionException ->
                        "Esse e-mail já possui uma conta."
                    else -> failure.localizedMessage ?: "Não foi possível concluir."
                }
            throw IllegalStateException(message, failure)
        }
    }
}
