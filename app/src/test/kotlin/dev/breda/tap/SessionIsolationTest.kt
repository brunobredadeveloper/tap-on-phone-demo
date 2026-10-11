package dev.breda.tap

import org.junit.Assert.*
import org.junit.Test

class SessionIsolationTest {
    @Test
    fun accountKeysCannotEscapeStoragePaths() {
        val key = sessionNamespace("../../accounts/user@x.com")
        assertTrue(key.matches(Regex("[a-f0-9]{64}")))
    }

    @Test
    fun distinctAccountsCannotShareLocalNamespace() {
        assertNotEquals(sessionNamespace("account-A"), sessionNamespace("account-B"))
        assertNotEquals(sessionNamespace("local-demo"), sessionNamespace("account-A"))
    }

    @Test
    fun restartRestoresSameNamespace() {
        assertEquals(sessionNamespace("account-A"), sessionNamespace("account-A"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun missingIdentityIsRejected() {
        sessionNamespace(" ")
    }
}
