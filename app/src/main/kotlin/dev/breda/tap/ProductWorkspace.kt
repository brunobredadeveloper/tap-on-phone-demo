package dev.breda.tap

import android.content.Intent
import android.nfc.NfcAdapter
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.animation.core.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
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
 var amount by rememberSaveable {mutableStateOf("25,90")};var scenario by rememberSaveable {mutableStateOf("approve")};var selected by rememberSaveable {mutableStateOf("")};var query by rememberSaveable {mutableStateOf("")};var filter by rememberSaveable {mutableStateOf("Todas")}
 Workspace(account,listOf("Terminal","Vendas","Conectar","Conta")){route,go->
  val completed=vm.state as? CheckoutState.Completed
  LaunchedEffect(vm.state,route){if(route=="operation" && vm.state is CheckoutState.Completed)go("result")}
  when(route){
   "home"->Page("Terminal do lojista"){Hero("TAP / SIMULAÇÃO",money(vm.sales.filter {it.status=="approved"}.sumOf {it.amount}),"Total aprovado no histórico fictício desta conta");TapOrbit();Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){Metric("Vendas",vm.sales.count {it.status=="approved"}.toString(),Modifier.weight(1f));Metric("A conciliar",vm.sales.count {it.status=="unknown"}.toString(),Modifier.weight(1f))};Button(onClick={vm.prepareNew();go(if(vm.state is CheckoutState.Completed && completed?.result==PaymentResult.OutcomeUnknown)"result" else "operation")},modifier=Modifier.fillMaxWidth()){Text(if(completed?.result==PaymentResult.OutcomeUnknown)"Resolver cobrança pendente" else "Nova cobrança")};ActionTile("Histórico do terminal","Resultados, recibos e busca",Icons.Default.ReceiptLong){go("activity")};Text(if(vm.useBackend)"Merchant Console conectado por URL" else "Simulador local ativo")}
   "operation"->Page("Cobrar com aproximação",{vm.cancelWaiting();go("home")}){when(val state=vm.state){
    is CheckoutState.Waiting->{TapOrbit();Hero("AGUARDANDO",money(state.request.amountInCents),"Toque abaixo para simular a aproximação");Button(vm::simulateTap,Modifier.fillMaxWidth()){Text("Simular aproximação")};OutlinedButton(onClick={vm.cancelWaiting();go("home")}){Text("Cancelar cobrança")}}
    is CheckoutState.Processing->StatusPanel("loading","Processando pagamento","Mantenha a tela aberta. Aguarde o resultado do gateway.")
    else->{OutlinedTextField(amount,{amount=it},label={Text("Valor em reais")},modifier=Modifier.fillMaxWidth());listOf("approve","decline","lost_response").forEach {value->FilterChip(selected=scenario==value,onClick={scenario=value},enabled=vm.useBackend||value!="lost_response",label={Text(when(value){"approve"->"Aprovação";"decline"->"Recusa";else->"Resposta perdida"})})};Text(if(vm.useBackend)"Cobrança fictícia no Merchant Console" else "Simulação inteiramente local");Button(onClick={vm.beginCheckout(amount,vm.useBackend,vm.endpoint,scenario)},modifier=Modifier.fillMaxWidth()){Text("Preparar cobrança")};Text(vm.message)}
   }}
   "result"->Page("Resultado da cobrança",{go("home")}){val result=completed?.result;StatusPanel(if(vm.consulting)"loading" else if(result is PaymentResult.Approved)"success" else "error",when(result){is PaymentResult.Approved->"Pagamento aprovado";is PaymentResult.Declined->"Pagamento recusado";else->"Resultado incerto"},vm.message);completed?.let {Text("Chave de reconciliação: ${it.request.idempotencyKey}")};if(result==PaymentResult.OutcomeUnknown)Button(onClick={vm.reconcile(vm.endpoint)},enabled=!vm.consulting,modifier=Modifier.fillMaxWidth()){Text("Consultar sem duplicar a cobrança")};if(result is PaymentResult.Approved){Text("Recibo: ${result.receipt.transactionId}");Button(onClick={selected=completed!!.request.idempotencyKey;go("details")},modifier=Modifier.fillMaxWidth()){Text("Abrir comprovante")}};OutlinedButton(onClick={go("home")}){Text("Voltar ao terminal")}}
   "activity"->Page("Vendas do terminal"){OutlinedTextField(query,{query=it},label={Text("Buscar referência")},modifier=Modifier.fillMaxWidth());Row{listOf("Todas","Aprovadas","Incertas").forEach {f->FilterChip(filter==f,{filter=f},label={Text(f)})}};val sales=vm.sales.filter {(it.key.contains(query,true)||it.transactionId.contains(query,true))&&(filter=="Todas"||(filter=="Aprovadas"&&it.status=="approved")||(filter=="Incertas"&&it.status=="unknown"))};if(sales.isEmpty())Text("Nenhuma venda neste filtro.");sales.forEach {sale->ActionTile(money(sale.amount),"${sale.status} · ${java.util.Date(sale.at)}",Icons.Default.Receipt){selected=sale.key;go("details")}}}
   "details"->Page("Comprovante digital",{go("activity")}){val sale=vm.sales.find {it.key==selected};if(sale!=null){StatusPanel(if(sale.status=="approved")"success" else "error",money(sale.amount),"Situação: ${sale.status}");Text("Breda Tap / SIMULAÇÃO\nReferência: ${sale.key}\nTransação: ${sale.transactionId.ifBlank {"Não confirmada"}}\nData: ${java.util.Date(sale.at)}");Text("Não comprova pagamento real. Não contém dados de cartão.")};if(vm.firebaseEnabled)OutlinedButton(vm::refreshHistory){Text("Carregar histórico Firebase")};vm.history.take(5).forEach {Text(it)}}
   "tools"->Page("Conexão e dispositivo"){Hero("TERMINAL HEALTH",if(vm.useBackend)"Backend HTTP" else "Simulador local","Configure a integração antes de iniciar uma venda.");Row{Switch(vm.useBackend,{vm.useBackend=it;vm.saveConnection()},enabled=vm.state !is CheckoutState.Processing && vm.state !is CheckoutState.Waiting);Text("Usar Merchant Console")};OutlinedTextField(vm.endpoint,{vm.endpoint=it},label={Text("URL do servidor")},modifier=Modifier.fillMaxWidth());Button(vm::saveConnection){Text("Salvar conexão")};Text("USB: adb reverse tcp:8080 tcp:8080\nDepois use http://127.0.0.1:8080");val nfc=remember {NfcAdapter.getDefaultAdapter(context)};Metric("NFC do aparelho",if(nfc==null)"Não disponível" else if(nfc.isEnabled)"Ativado" else "Desativado",Modifier.fillMaxWidth());if(nfc!=null)OutlinedButton(onClick={runCatching {context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS))}}){Text("Abrir configuração NFC")};Text("NFC é apenas diagnóstico. A demo não lê cartões bancários nem realiza EMV.");Text(if(vm.firebaseEnabled)"Histórico Firebase disponível na conta autenticada" else "Histórico local: configure Firebase e entre para ativar a nuvem.")}
  }
 }
}
