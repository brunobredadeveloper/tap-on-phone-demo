package dev.breda.tap

/** Small ports allow authentication to be replaced without changing presentation. */
data class Account(val uid: String, val email: String, val name: String)

interface Authentication {
    val configured: Boolean

    fun observe(onChange: (Account?) -> Unit): AutoCloseable

    suspend fun login(email: String, password: String)

    suspend fun register(name: String, email: String, password: String)

    suspend fun reset(email: String)

    suspend fun verify()

    fun signOut()
}

interface SessionStore {
    var onboarded: Boolean

    fun select(uid: String)
}

/** Validation is reusable outside Compose and runs before an external request. */
object AccountValidation {
    fun login(email: String, password: String) {
        require(email.contains('@') && password.isNotBlank()) { "Informe e-mail e senha." }
    }

    fun register(name: String, email: String, password: String) {
        require(name.trim().length >= 2 && email.contains('@') && password.length >= 8) {
            "Use nome, e-mail válido e senha com pelo menos 8 caracteres."
        }
    }

    fun email(email: String) {
        require(email.contains('@')) { "Informe seu e-mail." }
    }
}
