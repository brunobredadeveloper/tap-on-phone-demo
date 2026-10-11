package dev.breda.tap

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ArchitectureContractTest {
    @Test
    fun amountRejectsAmbiguousAndOverflowingInputs() {
        listOf("0", "-1", "1.001", "1e2", "NaN", "92233720368547758.08", "").forEach {
            assertNull("Accepted $it", AmountInput.cents(it))
        }
        assertEquals(1234L, AmountInput.cents(" 12,34 "))
        assertEquals(1L, AmountInput.cents("0.01"))
    }

    @Test
    fun accountValidationRejectsInvalidCredentialsBeforeIo() {
        assertTrue(runCatching { AccountValidation.register("B", "invalid", "123") }.isFailure)
        assertTrue(runCatching { AccountValidation.login("invalid", "") }.isFailure)
        AccountValidation.register("Bruno", "bruno@example.com", "password123")
    }

    @Test
    fun authenticationIsInjectableAndDemoNeverCallsLogin() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val gateway = FakeAuthentication()
            val store = FakeSessionStore()
            val model = AccountSession(gateway, store)
            model.preview()
            assertEquals("local-demo", model.session)
            assertEquals("local-demo", store.uid)
            assertEquals(0, gateway.logins)
            model.logout()
            assertNull(model.session)
            model.login("bruno@example.com", "secret")
            advanceUntilIdle()
            assertEquals("user-1", model.session)
            assertEquals("user-1", store.uid)
            assertFalse(model.busy)
            assertEquals(1, gateway.logins)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class FakeSessionStore : SessionStore {
        override var onboarded = false
        var uid = ""

        override fun select(uid: String) {
            this.uid = uid
        }
    }

    private class FakeAuthentication : Authentication {
        override val configured = true
        var logins = 0
        private var change: (Account?) -> Unit = {}

        override fun observe(onChange: (Account?) -> Unit): AutoCloseable {
            change = onChange
            return AutoCloseable { change = {} }
        }

        override suspend fun login(email: String, password: String) {
            logins++
            change(Account("user-1", email, "Bruno"))
        }

        override suspend fun register(name: String, email: String, password: String) = Unit

        override suspend fun reset(email: String) = Unit

        override suspend fun verify() = Unit

        override fun signOut() {
            change(null)
        }
    }
}
