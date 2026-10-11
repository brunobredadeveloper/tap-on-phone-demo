package dev.breda.tap

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.security.MessageDigest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

fun sessionNamespace(uid: String): String {
    require(uid.isNotBlank())
    return MessageDigest.getInstance("SHA-256").digest(uid.toByteArray()).joinToString("") {
        "%02x".format(it)
    }
}

/** Presentation state only; authentication and persistence are injected ports. */
class AccountSession(
    private val authentication: Authentication,
    private val sessionStore: SessionStore,
) : ViewModel() {
    val configured
        get() = authentication.configured

    var session by mutableStateOf<String?>(null)
        private set

    var email by mutableStateOf("")
        private set

    var name by mutableStateOf("")
        private set

    var busy by mutableStateOf(false)
        private set

    var error by mutableStateOf("")
        private set

    var notice by mutableStateOf("")
        private set

    var onboarded by mutableStateOf(sessionStore.onboarded)
        private set

    val demo
        get() = session == "local-demo"

    private val subscription =
        authentication.observe { user ->
            if (user != null) {
                sessionStore.select(user.uid)
                email = user.email
                name = user.name
                session = user.uid
            } else if (!demo) {
                sessionStore.select("local-demo")
                session = null
                email = ""
                name = ""
            }
        }

    fun finishOnboarding() {
        sessionStore.onboarded = true
        onboarded = true
    }

    fun preview() {
        authentication.signOut()
        sessionStore.select("local-demo")
        session = "local-demo"
        name = "Bruno · demonstração"
        email = "Ambiente local"
        error = ""
    }

    fun login(email: String, password: String) = perform { authentication.login(email, password) }

    fun register(name: String, email: String, password: String) = perform {
        authentication.register(name, email, password)
        this@AccountSession.name = name.trim()
        notice = "Conta criada."
    }

    fun reset(email: String) = perform {
        authentication.reset(email)
        notice = "Se existir uma conta para esse e-mail, você receberá as instruções."
    }

    fun verify() = perform {
        authentication.verify()
        notice = "E-mail de verificação enviado."
    }

    fun logout() {
        authentication.signOut()
        sessionStore.select("local-demo")
        session = null
        name = ""
        email = ""
        error = ""
        notice = ""
    }

    private fun perform(block: suspend () -> Unit) {
        if (busy) return
        busy = true
        error = ""
        notice = ""
        viewModelScope.launch {
            try {
                block()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                error = failure.localizedMessage ?: "Não foi possível concluir."
            } finally {
                busy = false
            }
        }
    }

    override fun onCleared() {
        subscription.close()
    }
}
