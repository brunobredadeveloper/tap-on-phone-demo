package dev.breda.tap

import android.content.Intent
import android.nfc.NfcAdapter
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.animation.core.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.breda.payments.PaymentResult
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale
private fun money(cents:Long)=NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(BigDecimal.valueOf(cents,2))
@Composable private fun TapOrbit(){val transition=rememberInfiniteTransition(label="tap-rings");val pulse by transition.animateFloat(0.4f,1f,infiniteRepeatable(tween(2200),RepeatMode.Restart),label="pulse");Box(Modifier.fillMaxWidth().height(190.dp),contentAlignment=Alignment.Center){Canvas(Modifier.size(170.dp)){drawCircle(Brand.copy(alpha=1f-pulse),radius=size.minDimension*pulse/2,style=Stroke(5f));drawCircle(Brand.copy(alpha=.3f),radius=size.minDimension/3,style=Stroke(2f))};Icon(Icons.Default.Contactless,"Aproximação simulada",Modifier.size(65.dp),tint=Brand)}}

@Composable fun ProductWorkspace(account:AccountSession){
 val vm:TapViewModel=viewModel(key="tap-${account.session}");val context=LocalContext.current
 val lifecycle=LocalLifecycleOwner.current
 var readiness by remember {mutableStateOf(DeviceReadiness.UNSUPPORTED)}
 var simulationConsent by rememberSaveable {mutableStateOf(false)}
 var checked by rememberSaveable {mutableStateOf(false)}
 DisposableEffect(lifecycle){
  fun refresh(){val adapter=NfcAdapter.getDefaultAdapter(context);readiness=deviceReadiness(adapter!=null,adapter?.isEnabled==true)}
  refresh();val observer=LifecycleEventObserver{_,event->if(event==Lifecycle.Event.ON_RESUME)refresh()};lifecycle.lifecycle.addObserver(observer)
  onDispose{lifecycle.lifecycle.removeObserver(observer)}
 }
 var amount by rememberSaveable {mutableStateOf("25,90")};var scenario by rememberSaveable {mutableStateOf("approve")};var selected by rememberSaveable {mutableStateOf("")};var query by rememberSaveable {mutableStateOf("")};var filter by rememberSaveable {mutableStateOf("Todas")}
 Workspace(account,listOf("Terminal","Vendas","Conectar","Conta")){route,go->
  val completed=vm.state as? CheckoutState.Completed
  LaunchedEffect(readiness){if(!checked){checked=true;if(readiness!=DeviceReadiness.READY)go("compatibility")}}
  fun startCheckout(){if(readiness!=DeviceReadiness.READY&&!simulationConsent)go("compatibility")else{vm.prepareNew();go(if(vm.state is CheckoutState.Completed && completed?.result==PaymentResult.OutcomeUnknown)"result" else "operation")}}
  LaunchedEffect(vm.state,route){if(route=="operation" && vm.state is CheckoutState.Completed)go("result")}
  when(route){
   "compatibility"->DeviceCompatibility(readiness,onSettings={runCatching{context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS))}},onSimulation={simulationConsent=true;go("home")},onBack={go("home")})
   "home"->Page("Terminal do lojista"){
    if(readiness!=DeviceReadiness.READY)Surface(color=MaterialTheme.colorScheme.errorContainer,shape=androidx.compose.foundation.shape.RoundedCornerShape(20.dp)){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text(if(readiness==DeviceReadiness.UNSUPPORTED)"Este celular não tem NFC" else "Ative o NFC para aproximar",fontWeight=FontWeight.Bold);Text(if(simulationConsent)"Modo de simulação ativo. Nenhuma venda usa NFC." else "Confira a compatibilidade antes de iniciar.");TextButton(onClick={go("compatibility")}){Text("Ver compatibilidade")}}}
Hero("TAP / SIMULAÇÃO",money(vm.sales.filter {it.status=="approved"}.sumOf {it.amount}),"Total aprovado no histórico fictício desta conta");TapOrbit();Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){Metric("Vendas",vm.sales.count {it.status=="approved"}.toString(),Modifier.weight(1f));Metric("A conciliar",vm.sales.count {it.status=="unknown"}.toString(),Modifier.weight(1f))};Button(onClick={startCheckout()},modifier=Modifier.fillMaxWidth()){Text(if(completed?.result==PaymentResult.OutcomeUnknown)"Resolver cobrança pendente" else "Nova cobrança")};ActionTile("Histórico do terminal","Resultados, recibos e busca",Icons.Default.ReceiptLong){go("activity")};Text(if(vm.useBackend)"Merchant Console conectado por URL" else "Simulador local ativo")}
   "operation"->Page("Cobrar com aproximação",{vm.cancelWaiting();go("home")}){when(val state=vm.state){
    is CheckoutState.Waiting->{TapOrbit();Hero("AGUARDANDO",money(state.request.amountInCents),"Toque abaixo para simular a aproximação");Button(vm::simulateTap,Modifier.fillMaxWidth()){Text("Simular aproximação")};OutlinedButton(onClick={vm.cancelWaiting();go("home")}){Text("Cancelar cobrança")}}
    is CheckoutState.Processing->ResultExperience("loading","Processando sua venda","Aguarde a resposta do gateway.",amount=money(state.request.amountInCents))
    else->{OutlinedTextField(amount,{amount=it},label={Text("Valor em reais")},modifier=Modifier.fillMaxWidth());listOf("approve","decline","lost_response").forEach {value->FilterChip(selected=scenario==value,onClick={scenario=value},enabled=vm.useBackend||value!="lost_response",label={Text(when(value){"approve"->"Aprovação";"decline"->"Recusa";else->"Resposta perdida"})})};Text(if(vm.useBackend)"Cobrança fictícia no Merchant Console" else "Simulação inteiramente local");Button(onClick={vm.beginCheckout(amount,vm.useBackend,vm.endpoint,scenario)},modifier=Modifier.fillMaxWidth()){Text("Preparar cobrança")};Text(vm.message)}
   }}
   "result"->{val result=completed?.result;ResultExperience(if(vm.consulting)"loading" else if(result is PaymentResult.Approved)"success" else "error",when{vm.consulting->"Consultando sua cobrança";result is PaymentResult.Approved->"Pagamento aprovado!";result is PaymentResult.Declined->"Pagamento recusado";else->"Precisamos confirmar o resultado"},when{vm.consulting->"Buscando a resposta sem realizar uma nova cobrança.";result is PaymentResult.Approved->"Tudo certo. O comprovante da venda está disponível.";result is PaymentResult.Declined->"A simulação foi recusada. Você pode iniciar uma nova venda.";else->"Não crie outra cobrança ainda. Consulte esta operação para evitar duplicidade."},amount=completed?.request?.amountInCents?.let(::money).orEmpty(),reference=completed?.request?.idempotencyKey.orEmpty(),primaryLabel=if(result==PaymentResult.OutcomeUnknown)"Consultar esta cobrança" else if(result is PaymentResult.Approved)"Ver comprovante" else "Voltar ao terminal",onPrimary={if(result==PaymentResult.OutcomeUnknown)vm.reconcile(vm.endpoint)else if(result is PaymentResult.Approved){selected=completed!!.request.idempotencyKey;go("details")}else go("home")},secondaryLabel="Ir para o terminal",onSecondary={go("home")})}
   "activity"->Page("Vendas do terminal"){OutlinedTextField(query,{query=it},label={Text("Buscar referência")},modifier=Modifier.fillMaxWidth());Row{listOf("Todas","Aprovadas","Incertas").forEach {f->FilterChip(filter==f,{filter=f},label={Text(f)})}};val sales=vm.sales.filter {(it.key.contains(query,true)||it.transactionId.contains(query,true))&&(filter=="Todas"||(filter=="Aprovadas"&&it.status=="approved")||(filter=="Incertas"&&it.status=="unknown"))};if(sales.isEmpty())Text("Nenhuma venda neste filtro.");sales.forEach {sale->ActionTile(money(sale.amount),"${sale.status} · ${java.util.Date(sale.at)}",Icons.Default.Receipt){selected=sale.key;go("details")}}}
   "details"->Page("Comprovante digital",{go("activity")}){val sale=vm.sales.find {it.key==selected};if(sale!=null){StatusPanel(if(sale.status=="approved")"success" else "error",money(sale.amount),"Situação: ${sale.status}");Text("Breda Tap / SIMULAÇÃO\nReferência: ${sale.key}\nTransação: ${sale.transactionId.ifBlank {"Não confirmada"}}\nData: ${java.util.Date(sale.at)}");Text("Não comprova pagamento real. Não contém dados de cartão.")};if(vm.firebaseEnabled)OutlinedButton(vm::refreshHistory){Text("Carregar histórico Firebase")};vm.history.take(5).forEach {Text(it)}}
   "tools"->Page("Conexão e dispositivo"){Hero("TERMINAL HEALTH",if(vm.useBackend)"Backend HTTP" else "Simulador local","Configure a integração antes de iniciar uma venda.");Row{Switch(vm.useBackend,{vm.useBackend=it;vm.saveConnection()},enabled=vm.state !is CheckoutState.Processing && vm.state !is CheckoutState.Waiting);Text("Usar Merchant Console")};OutlinedTextField(vm.endpoint,{vm.endpoint=it},label={Text("URL do servidor")},modifier=Modifier.fillMaxWidth());Button(vm::saveConnection){Text("Salvar conexão")};Text("USB: adb reverse tcp:8080 tcp:8080\nDepois use http://127.0.0.1:8080");val nfc=remember {NfcAdapter.getDefaultAdapter(context)};Metric("NFC do aparelho",if(nfc==null)"Não disponível" else if(nfc.isEnabled)"Ativado" else "Desativado",Modifier.fillMaxWidth());if(nfc!=null)OutlinedButton(onClick={runCatching {context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS))}}){Text("Abrir configuração NFC")};Text("NFC é apenas diagnóstico. A demo não lê cartões bancários nem realiza EMV.");Text(if(vm.firebaseEnabled)"Histórico Firebase disponível na conta autenticada" else "Histórico local: configure Firebase e entre para ativar a nuvem.")}
  }
 }
}

@Composable private fun DeviceCompatibility(readiness:DeviceReadiness,onSettings:()->Unit,onSimulation:()->Unit,onBack:()->Unit){
 Column(Modifier.fillMaxSize().testTag("device-compatibility").verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(26.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(18.dp)){
  Spacer(Modifier.height(20.dp));BrandMark(60.dp);MotionState(if(readiness==DeviceReadiness.READY)"success" else "error",size=220.dp)
  Text(when(readiness){DeviceReadiness.UNSUPPORTED->"Este celular não aceita pagamentos por aproximação";DeviceReadiness.DISABLED->"Seu NFC está desativado";DeviceReadiness.READY->"NFC ativado"},style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold,textAlign=androidx.compose.ui.text.style.TextAlign.Center)
  Text(when(readiness){DeviceReadiness.UNSUPPORTED->"Não encontramos NFC neste aparelho. Para pagamentos por aproximação, use outro celular compatível com NFC.";DeviceReadiness.DISABLED->"O aparelho tem NFC. Ative-o nas configurações para deixar a aproximação disponível.";DeviceReadiness.READY->"O aparelho tem NFC ativado. Nesta versão, as cobranças continuam sendo simuladas."},textAlign=androidx.compose.ui.text.style.TextAlign.Center,color=MaterialTheme.colorScheme.onSurfaceVariant)
  if(readiness==DeviceReadiness.DISABLED)Button(onSettings,Modifier.fillMaxWidth().heightIn(min=56.dp)){Text("Ativar NFC nas configurações")}
  if(readiness==DeviceReadiness.READY)Button(onBack,Modifier.fillMaxWidth().heightIn(min=56.dp)){Text("Voltar ao terminal")}
  OutlinedButton(onSimulation,Modifier.fillMaxWidth().heightIn(min=56.dp)){Text("Usar somente a simulação")}
  Text("O diagnóstico NFC verifica o aparelho. Este aplicativo não processa pagamentos reais, mesmo com NFC disponível.",style=MaterialTheme.typography.bodySmall,textAlign=androidx.compose.ui.text.style.TextAlign.Center)
 }
}
