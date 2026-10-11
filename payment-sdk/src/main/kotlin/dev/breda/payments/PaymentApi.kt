package dev.breda.payments

/** BRL minor units. Validation is performed by PaymentClient so bad input is a typed result. */
public data class PaymentRequest(public val amountInCents: Long, public val idempotencyKey: String)

public data class Receipt(public val transactionId: String, public val amountInCents: Long)

public enum class DeclineReason {
    INSUFFICIENT_FUNDS,
    REJECTED_BY_ISSUER,
}

public sealed interface PaymentResult {
    public data class Approved(public val receipt: Receipt) : PaymentResult

    public data class Declined(public val reason: DeclineReason) : PaymentResult

    public data object InvalidRequest : PaymentResult

    public data object IdempotencyConflict : PaymentResult

    /** Timeout is uncertain: a real gateway may have already charged the payer. */
    public data object OutcomeUnknown : PaymentResult

    public data object GatewayUnavailable : PaymentResult

    public data object SessionCapacityReached : PaymentResult
}

/**
 * Gateway implementations must enforce idempotency remotely and propagate coroutine cancellation.
 */
public fun interface PaymentGateway {
    public suspend fun authorize(request: PaymentRequest): PaymentResult
}
