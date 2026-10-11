package dev.breda.tap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.breda.payments.PaymentResult

class MainActivity:ComponentActivity(){override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent {MaterialTheme {Surface {TapScreen()}}}}}
@Composable fun TapScreen(vm:TapViewModel=viewModel()){
 var amount by remember {mutableStateOf("25,90")};var url by remember {mutableStateOf("http://10.0.2.2:8080")};var backend by remember {mutableStateOf(false)};var scenario by remember {mutableStateOf("approve")}
 val busy=vm.state is CheckoutState.Waiting || vm.state is CheckoutState.Processing
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
  Text("Breda Tap",style=MaterialTheme.typography.headlineLarge);Text("Demonstração · aproximação simulada · sem cartões reais")
  OutlinedTextField(amount,{amount=it},label={Text("Valor em reais")},enabled=!busy,modifier=Modifier.fillMaxWidth())
  Row {Switch(backend,{backend=it},enabled=!busy);Text(" Usar Merchant Console")}
  if(backend)OutlinedTextField(url,{url=it},label={Text("URL do backend local")},enabled=!busy,modifier=Modifier.fillMaxWidth())
  Column {listOf("approve","decline","lost_response").forEach {value->FilterChip(selected=scenario==value,onClick={scenario=value},enabled=!busy && (backend || value!="lost_response"),label={Text(when(value){"approve"->"Aprovar";"decline"->"Recusar";else->"Timeout"})})}}
  Button(onClick={vm.beginCheckout(amount,backend,url,scenario)},enabled=!busy,modifier=Modifier.fillMaxWidth()){Text("Iniciar cobrança")}
  if(vm.state is CheckoutState.Waiting)Button(onClick=vm::simulateTap,modifier=Modifier.fillMaxWidth()){Text("Simular aproximação")}
  if(vm.state is CheckoutState.Processing)CircularProgressIndicator()
  Text(vm.message,style=MaterialTheme.typography.bodyLarge)
  if((vm.state as? CheckoutState.Completed)?.result == PaymentResult.OutcomeUnknown)OutlinedButton(onClick={vm.reconcile(url)},enabled=true){Text("Consultar resultado")}
  HorizontalDivider();Text(if(vm.firebaseEnabled)"Firebase configurado" else "Histórico local · Firebase não configurado")
  if(vm.firebaseEnabled)OutlinedButton(onClick=vm::refreshHistory){Text("Carregar histórico Firebase")}
  vm.history.forEach {Text(it)}
 }
}
