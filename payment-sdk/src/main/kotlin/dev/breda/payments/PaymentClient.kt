package dev.breda.payments

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/** Session-scoped client. The execution timeout starts once this request obtains the lock. */
public class PaymentClient(
 private val gateway: PaymentGateway,
 private val timeoutMillis: Long = 5_000,
 private val maxSessionRequests: Int = 1_000,
) {
 init { require(timeoutMillis > 0); require(maxSessionRequests > 0) }
 private val mutex = Mutex()
 private val completed = mutableMapOf<String, Pair<PaymentRequest, PaymentResult>>()

 /**
  * Exact retries replay their result; reuse with a changed amount conflicts.
  * Caller cancellation propagates. No terminal result is cached on caller cancellation.
  * Keep and reuse the request key with a real backend, even after caller cancellation.
  */
 public suspend fun pay(request: PaymentRequest): PaymentResult {
  if (request.amountInCents <= 0 || request.idempotencyKey.isBlank() || request.idempotencyKey.length > 128) {
   return PaymentResult.InvalidRequest
  }
  return mutex.withLock {
   completed[request.idempotencyKey]?.let { (original, result) ->
    return@withLock if (original == request) result else PaymentResult.IdempotencyConflict
   }
   if (completed.size >= maxSessionRequests) return@withLock PaymentResult.SessionCapacityReached
   val result = try {
    withTimeoutOrNull(timeoutMillis) { gateway.authorize(request) } ?: PaymentResult.OutcomeUnknown
   } catch (cancelled: CancellationException) {
    throw cancelled
   } catch (_: Exception) {
    PaymentResult.OutcomeUnknown
   }
   completed[request.idempotencyKey] = request to result
   result
  }
 }
}
