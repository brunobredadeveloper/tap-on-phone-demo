package dev.breda.tap
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.*
import dev.breda.payments.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.UUID

class TapViewModel(app: Application): AndroidViewModel(app) {
 var state by mutableStateOf<CheckoutState>(CheckoutState.Idle);private set
 var message by mutableStateOf("Pronto para uma venda fictícia");private set
 var history by mutableStateOf(listOf<String>());private set
 private val cloud=FirebaseHistory(app)
 val firebaseEnabled get()=cloud.configured
 private var client: PaymentClient?=null
 private var remote: HttpGateway?=null
 private var selectedScenario="approve"
 private var selectedBackend=false
 private val prefs=app.getSharedPreferences("checkout",0)
 init {
  val key=prefs.getString("pendingKey",null);val amount=prefs.getLong("pendingAmount",0)
  if(key!=null && amount>0){state=CheckoutState.Completed(PaymentRequest(amount,key),PaymentResult.OutcomeUnknown);message="Cobrança pendente recuperada. Consulte o backend antes de uma nova cobrança."}
 }
 fun beginCheckout(input:String,backend:Boolean,url:String,scenario:String){
  if(state is CheckoutState.Waiting || state is CheckoutState.Processing)return
  if(prefs.contains("pendingKey")){message="Concilie a cobrança pendente primeiro.";return}
  val amount=parseCents(input) ?: run {message="Informe um valor válido, com até duas casas decimais";return}
  try {
   selectedScenario=scenario;selectedBackend=backend
   remote=if(backend) HttpGateway(url,scenario) else null
   val gateway:PaymentGateway=remote ?: SimulatedPaymentGateway(if(scenario=="decline") SimulationScenario.DECLINE else SimulationScenario.APPROVE)
   client=PaymentClient(gateway)
   val request=PaymentRequest(amount,UUID.randomUUID().toString());state=begin(state,request);message="Aguardando aproximação simulada"
  }catch(e:Exception){message=e.message ?: "Configuração inválida"}
 }
 fun simulateTap(){
  val waiting=state as? CheckoutState.Waiting ?: return;state=tap(state)
  prefs.edit().putString("pendingKey",waiting.request.idempotencyKey).putLong("pendingAmount",waiting.request.amountInCents).putBoolean("pendingBackend",selectedBackend).putString("pendingScenario",selectedScenario).commit()
  viewModelScope.launch {
   val result=requireNotNull(client).pay(waiting.request);state=finish(state,waiting.request.idempotencyKey,result);record(waiting.request,result)
  }
 }
 fun reconcile(url:String){
  val completed=state as? CheckoutState.Completed ?: return
  viewModelScope.launch {
   try {val result=if(prefs.getBoolean("pendingBackend",true)) HttpGateway(url,"approve").reconcile(completed.request.idempotencyKey) else SimulatedPaymentGateway(if(prefs.getString("pendingScenario","approve")=="decline") SimulationScenario.DECLINE else SimulationScenario.APPROVE).authorize(completed.request);state=CheckoutState.Completed(completed.request,result);record(completed.request,result)}
   catch(e:CancellationException){throw e}catch(e:Exception){message="Consulta falhou: ${e.message}"}
  }
 }
 private suspend fun record(request:PaymentRequest,result:PaymentResult){
  val status=when(result){is PaymentResult.Approved->"approved";is PaymentResult.Declined->"declined";else->"unknown"}
  message=when(result){is PaymentResult.Approved->"Aprovado · ${result.receipt.transactionId}";is PaymentResult.Declined->"Recusado";else->"Resultado incerto: consulte a cobrança"}
  if(status!="unknown")prefs.edit().remove("pendingKey").remove("pendingAmount").commit()
  history=listOf("${request.amountInCents} centavos · $status")+history
  if(cloud.configured)try{cloud.save(request.idempotencyKey,request.amountInCents,status)}catch(e:CancellationException){throw e}catch(e:Exception){message+=" · Histórico Firebase pendente: ${e.message}"}
 }
 fun refreshHistory(){if(!cloud.configured)return;viewModelScope.launch {try{history=cloud.history()}catch(e:CancellationException){throw e}catch(e:Exception){message="Firebase: ${e.message}"}}}
}
