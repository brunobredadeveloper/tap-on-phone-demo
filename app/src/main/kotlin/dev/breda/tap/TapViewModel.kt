package dev.breda.tap
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.*
import dev.breda.payments.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.UUID

data class Sale(val key:String,val amount:Long,val status:String,val transactionId:String,val at:Long=System.currentTimeMillis())

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
 private var checkoutEndpoint=""
 private val namespace=app.getSharedPreferences("portfolio-account",0).getString("namespace",sessionNamespace("local-demo"))!!
 private val prefs=app.getSharedPreferences("checkout-$namespace",0)
 var sales by mutableStateOf(listOf<Sale>());private set
 var consulting by mutableStateOf(false);private set
 var endpoint by mutableStateOf(prefs.getString("endpoint","http://10.0.2.2:8080")!!)
 var useBackend by mutableStateOf(prefs.getBoolean("useBackend",false))
 fun saveConnection(){prefs.edit().putString("endpoint",endpoint).putBoolean("useBackend",useBackend).apply()}

 init {
  sales=runCatching {val list=org.json.JSONArray(prefs.getString("sales","[]"));(0 until list.length()).map {i->val x=list.getJSONObject(i);Sale(x.getString("key"),x.getLong("amount"),x.getString("status"),x.optString("transactionId"),x.getLong("at"))}}.getOrDefault(emptyList())
  history=sales.map {"${it.amount} centavos · ${it.status}"}
  val key=prefs.getString("pendingKey",null);val amount=prefs.getLong("pendingAmount",0)
  if(key!=null && amount>0){state=CheckoutState.Completed(PaymentRequest(amount,key),PaymentResult.OutcomeUnknown);message="Cobrança pendente recuperada. Consulte o backend antes de uma nova cobrança."}
 }
 fun beginCheckout(input:String,backend:Boolean,url:String,scenario:String){
  if(state is CheckoutState.Waiting || state is CheckoutState.Processing || consulting)return
  if(prefs.contains("pendingKey")){message="Concilie a cobrança pendente primeiro.";return}
  val amount=parseCents(input) ?: run {message="Informe um valor válido, com até duas casas decimais";return}
  try {
   selectedScenario=scenario;selectedBackend=backend;checkoutEndpoint=url
   remote=if(backend) HttpGateway(url,scenario) else null
   val gateway:PaymentGateway=remote ?: SimulatedPaymentGateway(if(scenario=="decline") SimulationScenario.DECLINE else SimulationScenario.APPROVE)
   client=PaymentClient(gateway)
   val request=PaymentRequest(amount,UUID.randomUUID().toString());state=begin(state,request);message="Aguardando aproximação simulada"
  }catch(e:Exception){message=e.message ?: "Configuração inválida"}
 }
 fun simulateTap(){
  val waiting=state as? CheckoutState.Waiting ?: return;state=tap(state)
  prefs.edit().putString("pendingKey",waiting.request.idempotencyKey).putLong("pendingAmount",waiting.request.amountInCents).putBoolean("pendingBackend",selectedBackend).putString("pendingScenario",selectedScenario).putString("pendingEndpoint",checkoutEndpoint).commit()
  viewModelScope.launch {
   val result=requireNotNull(client).pay(waiting.request);state=finish(state,waiting.request.idempotencyKey,result);record(waiting.request,result)
  }
 }
 fun reconcile(url:String){
  val completed=state as? CheckoutState.Completed ?: return
  if(consulting)return
  consulting=true
  viewModelScope.launch {
   try {val result=if(prefs.getBoolean("pendingBackend",true)) HttpGateway(prefs.getString("pendingEndpoint",url) ?: url,"approve").reconcile(completed.request.idempotencyKey) else SimulatedPaymentGateway(if(prefs.getString("pendingScenario","approve")=="decline") SimulationScenario.DECLINE else SimulationScenario.APPROVE).authorize(completed.request);state=CheckoutState.Completed(completed.request,result);record(completed.request,result)}
   catch(e:CancellationException){throw e}catch(e:Exception){message="Consulta falhou: ${e.message}"}finally{consulting=false}
  }
 }
 private suspend fun record(request:PaymentRequest,result:PaymentResult){
  val status=when(result){is PaymentResult.Approved->"approved";is PaymentResult.Declined->"declined";else->"unknown"}
  message=when(result){is PaymentResult.Approved->"Aprovado · ${result.receipt.transactionId}";is PaymentResult.Declined->"Recusado";else->"Resultado incerto: consulte a cobrança"}
  if(status!="unknown")prefs.edit().remove("pendingKey").remove("pendingAmount").commit()
  val sale=Sale(request.idempotencyKey,request.amountInCents,status,(result as? PaymentResult.Approved)?.receipt?.transactionId.orEmpty())
  sales=listOf(sale)+sales.filterNot {it.key==request.idempotencyKey}
  val rows=org.json.JSONArray();sales.forEach {x->rows.put(org.json.JSONObject().put("key",x.key).put("amount",x.amount).put("status",x.status).put("transactionId",x.transactionId).put("at",x.at))};prefs.edit().putString("sales",rows.toString()).commit()
  history=sales.map {"${it.amount} centavos · ${it.status}"}
  if(cloud.configured)try{cloud.save(request.idempotencyKey,request.amountInCents,status)}catch(e:CancellationException){throw e}catch(e:Exception){message+=" · Histórico Firebase pendente: ${e.message}"}
 }
 fun prepareNew(){if(!prefs.contains("pendingKey") && state is CheckoutState.Completed)state=CheckoutState.Idle}
 fun cancelWaiting(){if(state is CheckoutState.Waiting){state=CheckoutState.Idle;message="Cobrança cancelada antes da aproximação."}}
 fun refreshHistory(){if(!cloud.configured)return;viewModelScope.launch {try{history=cloud.history()}catch(e:CancellationException){throw e}catch(e:Exception){message="Firebase: ${e.message}"}}}
}
