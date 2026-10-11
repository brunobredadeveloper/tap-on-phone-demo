package dev.breda.tap

import dev.breda.payments.*

data class PendingCheckout(
    val request: PaymentRequest,
    val backend: Boolean,
    val scenario: String,
    val endpoint: String,
)

data class CheckoutConnection(
    val endpoint: String = "http://10.0.2.2:8080",
    val backend: Boolean = false,
)

interface CheckoutStorage {
    var connection: CheckoutConnection
    var pending: PendingCheckout?
    var sales: List<Sale>
}

interface SaleHistory {
    val configured: Boolean

    suspend fun save(key: String, amount: Long, status: String)

    suspend fun history(): List<String>
}

interface CheckoutGatewayFactory {
    fun create(backend: Boolean, endpoint: String, scenario: String): PaymentGateway

    suspend fun reconcile(pending: PendingCheckout): PaymentResult
}

class DemoCheckoutGateways : CheckoutGatewayFactory {
    override fun create(backend: Boolean, endpoint: String, scenario: String): PaymentGateway =
        if (backend) HttpGateway(endpoint, scenario)
        else
            SimulatedPaymentGateway(
                if (scenario == "decline") SimulationScenario.DECLINE
                else SimulationScenario.APPROVE
            )

    override suspend fun reconcile(pending: PendingCheckout): PaymentResult =
        if (pending.backend)
            HttpGateway(pending.endpoint, "approve").reconcile(pending.request.idempotencyKey)
        else create(false, pending.endpoint, pending.scenario).authorize(pending.request)
}

/** Persist before authorization, and commit the ledger before clearing recovery data. */
class CheckoutRecovery(private val storage: CheckoutStorage) {
    fun start(pending: PendingCheckout) {
        check(storage.pending == null) { "Concilie a cobrança pendente primeiro." }
        storage.pending = pending
    }

    fun record(request: PaymentRequest, result: PaymentResult): Sale {
        val status =
            when (result) {
                is PaymentResult.Approved -> "approved"
                is PaymentResult.Declined -> "declined"
                else -> "unknown"
            }
        val sale =
            Sale(
                request.idempotencyKey,
                request.amountInCents,
                status,
                (result as? PaymentResult.Approved)?.receipt?.transactionId.orEmpty(),
            )
        storage.sales = listOf(sale) + storage.sales.filterNot { it.key == sale.key }
        if (status != "unknown") storage.pending = null
        return sale
    }
}
