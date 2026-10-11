package dev.breda.tap
import dev.breda.payments.*
import kotlin.test.*
class CheckoutTest {
 private val request=PaymentRequest(2590,"key")
 @Test fun exactMoney(){assertEquals(2590L,parseCents("25,90"));assertNull(parseCents("1.001"));assertNull(parseCents("-1"));assertNull(parseCents("NaN"))}
 @Test fun tapOnlyWhileWaiting(){assertEquals(CheckoutState.Idle,tap(CheckoutState.Idle));assertIs<CheckoutState.Processing>(tap(begin(CheckoutState.Idle,request)))}
 @Test fun duplicateTapDoesNotRestart(){val p=tap(begin(CheckoutState.Idle,request));assertEquals(p,tap(p));assertEquals(p,begin(p,PaymentRequest(10,"other")))}
 @Test fun staleResponseIgnored(){val p=tap(begin(CheckoutState.Idle,request));assertEquals(p,finish(p,"old",PaymentResult.OutcomeUnknown))}
 @Test fun timeoutPreservesRequestForReconciliation(){val p=tap(begin(CheckoutState.Idle,request));val c=finish(p,"key",PaymentResult.OutcomeUnknown) as CheckoutState.Completed;assertEquals(request,c.request)}
}
