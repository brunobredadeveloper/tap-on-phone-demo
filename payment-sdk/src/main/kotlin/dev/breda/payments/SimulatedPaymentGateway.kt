package dev.breda.payments

import kotlinx.coroutines.delay

public enum class SimulationScenario { APPROVE, DECLINE, UNAVAILABLE }
/** Deterministic simulator. Does not collect card details or connect to payment rails. */
public class SimulatedPaymentGateway(
 private val scenario: SimulationScenario = SimulationScenario.APPROVE,
 private val latencyMillis: Long = 800,
) : PaymentGateway {
 init { require(latencyMillis >= 0) }
 override suspend fun authorize(request: PaymentRequest): PaymentResult {
  delay(latencyMillis)
  return when (scenario) {
   SimulationScenario.APPROVE -> PaymentResult.Approved(Receipt("sim-${request.idempotencyKey}", request.amountInCents))
   SimulationScenario.DECLINE -> PaymentResult.Declined(DeclineReason.REJECTED_BY_ISSUER)
   SimulationScenario.UNAVAILABLE -> PaymentResult.GatewayUnavailable
  }
 }
}
