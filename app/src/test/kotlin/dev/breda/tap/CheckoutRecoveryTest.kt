package dev.breda.tap

import dev.breda.payments.*
import org.junit.Assert.*
import org.junit.Test

class CheckoutRecoveryTest {
    @Test
    fun unknownKeepsRecoveryAndDeclineClearsOnlyAfterLedgerSaved() {
        val storage = MemoryStorage()
        val recovery = CheckoutRecovery(storage)
        val request = PaymentRequest(100, "original-key")
        recovery.start(PendingCheckout(request, true, "approve", "http://localhost:8080"))
        assertTrue(
            runCatching { recovery.start(PendingCheckout(request, false, "approve", "")) }.isFailure
        )
        recovery.record(request, PaymentResult.OutcomeUnknown)
        assertNotNull(storage.pending)
        assertEquals("unknown", storage.sales.single().status)
        recovery.record(request, PaymentResult.Declined(DeclineReason.REJECTED_BY_ISSUER))
        assertNull(storage.pending)
        assertEquals(1, storage.sales.size)
        assertEquals("declined", storage.sales.single().status)
    }

    @Test
    fun failedLedgerWritePreservesPendingCharge() {
        val storage = MemoryStorage()
        val recovery = CheckoutRecovery(storage)
        val request = PaymentRequest(100, "key")
        recovery.start(PendingCheckout(request, true, "approve", "http://localhost:8080"))
        storage.failWrite = true
        assertTrue(
            runCatching {
                    recovery.record(
                        request,
                        PaymentResult.Declined(DeclineReason.REJECTED_BY_ISSUER),
                    )
                }
                .isFailure
        )
        assertEquals(request, storage.pending?.request)
    }

    private class MemoryStorage : CheckoutStorage {
        override var connection = CheckoutConnection()
        override var pending: PendingCheckout? = null
        var failWrite = false
        private var rows = emptyList<Sale>()
        override var sales: List<Sale>
            get() = rows
            set(value) {
                check(!failWrite)
                rows = value
            }
    }
}
