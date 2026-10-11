package dev.breda.tap
import dev.breda.payments.*
import java.math.BigDecimal

fun parseCents(input: String): Long? = try {
 BigDecimal(input.trim().replace(',', '.')).movePointRight(2).longValueExact().takeIf { it in 1..100_000_000 }
} catch (_: Exception) { null }
sealed interface CheckoutState {
 data object Idle : CheckoutState
 data class Waiting(val request: PaymentRequest) : CheckoutState
 data class Processing(val request: PaymentRequest) : CheckoutState
 data class Completed(val request: PaymentRequest, val result: PaymentResult) : CheckoutState
}
/** Pure state transitions ensure one simulated tap per checkout. */
fun begin(state: CheckoutState, request: PaymentRequest): CheckoutState =
 if (state is CheckoutState.Waiting || state is CheckoutState.Processing) state else CheckoutState.Waiting(request)
fun tap(state: CheckoutState): CheckoutState = if (state is CheckoutState.Waiting) CheckoutState.Processing(state.request) else state
fun finish(state: CheckoutState, key: String, result: PaymentResult): CheckoutState =
 if (state is CheckoutState.Processing && state.request.idempotencyKey == key) CheckoutState.Completed(state.request,result) else state
