package dev.breda.tap
import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.airbnb.lottie.compose.*
import kotlinx.coroutines.delay

val Brand=Color(0xFFFFB547)
val BrandTitle="Breda Tap"
@Composable fun BrandTheme(content:@Composable ()->Unit) {
 val colors=darkColorScheme(primary=Brand,background=Color(0xFF15100D),surface=Color(0xFF211E29),secondary=Color(0xFFFCD096))
 MaterialTheme(colorScheme=colors,shapes=Shapes(small=RoundedCornerShape(12.dp),medium=RoundedCornerShape(20.dp),large=RoundedCornerShape(28.dp)),content=content)
}
@Composable fun MotionState(kind:String,modifier:Modifier=Modifier) {
 val composition by rememberLottieComposition(LottieCompositionSpec.Asset("motion_$kind.json"))
 val progress by animateLottieCompositionAsState(composition,iterations=if(kind=="loading") LottieConstants.IterateForever else 1,restartOnPlay=true)
 LottieAnimation(composition,{progress},modifier.size(110.dp))
}
@Composable fun Hero(eyebrow:String,title:String,subtitle:String) {
 val infinite=rememberInfiniteTransition(label="hero")
 val glow by infinite.animateFloat(0.15f,0.32f,infiniteRepeatable(tween(3500),RepeatMode.Reverse),label="glow")
 Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Brand.copy(alpha=glow),MaterialTheme.colorScheme.surface)),RoundedCornerShape(28.dp)).padding(24.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
  Text(eyebrow.uppercase(),style=MaterialTheme.typography.labelMedium,color=Brand,fontWeight=FontWeight.Bold)
  Text(title,style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold)
  Text(subtitle,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
 }
}
@Composable fun Metric(label:String,value:String,modifier:Modifier=Modifier) {Card(modifier) {Column(Modifier.padding(20.dp)) {Text(label,style=MaterialTheme.typography.labelMedium);Text(value,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)}}}
@Composable fun ActionTile(title:String,subtitle:String,icon:ImageVector=Icons.Default.ArrowForward,onClick:()->Unit) {
 Card(onClick=onClick,modifier=Modifier.fillMaxWidth()) {Row(Modifier.padding(20.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {Icon(icon,null,tint=Brand);Column(Modifier.weight(1f)) {Text(title,fontWeight=FontWeight.SemiBold);Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)};Icon(Icons.Default.ChevronRight,null)}}
}
@Composable fun Page(title:String,back:(()->Unit)?=null,content:@Composable ColumnScope.()->Unit) {
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
  if(back!=null) TextButton(onClick=back) {Icon(Icons.AutoMirrored.Filled.ArrowBack,null);Text("Voltar")}
  Text(title,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);content()
 }
}
@Composable fun StatusPanel(kind:String,title:String,description:String) {
 Card(Modifier.fillMaxWidth()) {Column(Modifier.padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)) {MotionState(kind);Text(title,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Text(description,style=MaterialTheme.typography.bodyMedium)}}
}
@Composable fun ExperienceRoot(account:AccountSession=viewModel(),content:@Composable (AccountSession)->Unit) {
 var splash by remember {mutableStateOf(true)}
 LaunchedEffect(Unit) {delay(1000);splash=false}
 Surface(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
  AnimatedContent(targetState=when {splash->"splash";!account.onboarded->"intro";account.session==null->"auth";else->"app"},label="root") {screen->
   when(screen) {
    "splash"->Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally) {MotionState("loading");Text(BrandTitle,style=MaterialTheme.typography.headlineLarge)}
    "intro"->Page("Bem-vindo") {Hero("TAP / EXPERIENCE",BrandTitle,"Transforme uma intenção em uma venda.");MotionState("success");Text("Uma experiência de portfólio com fluxos completos e operações fictícias.");Text("Login Firebase identifica sua conta. O modo local funciona sem configuração e é claramente separado.");Button(account::finishOnboarding,Modifier.fillMaxWidth()) {Text("Começar")}}
    "auth"->AuthScreen(account)
    else->key(account.session) {content(account)}
   }
  }
 }
}
@Composable private fun AuthScreen(account:AccountSession) {
 var mode by rememberSaveable {mutableStateOf("login")};var email by rememberSaveable {mutableStateOf("")};var name by rememberSaveable {mutableStateOf("")};var password by remember {mutableStateOf("")}
 Page(when(mode) {"register"->"Crie sua conta";"reset"->"Recuperar acesso";else->"Entre na sua conta"}) {
  Hero("TAP",BrandTitle,"Transforme uma intenção em uma venda.")
  if(!account.configured) Text("Firebase ainda não configurado neste APK. Você pode explorar a demonstração local.",color=MaterialTheme.colorScheme.onSurfaceVariant)
  if(mode=="register") OutlinedTextField(name,{name=it},label={Text("Nome")},singleLine=true,modifier=Modifier.fillMaxWidth(),enabled=!account.busy)
  OutlinedTextField(email,{email=it},label={Text("E-mail")},singleLine=true,modifier=Modifier.fillMaxWidth(),enabled=!account.busy)
  if(mode!="reset") OutlinedTextField(password,{password=it},label={Text("Senha")},visualTransformation=PasswordVisualTransformation(),singleLine=true,modifier=Modifier.fillMaxWidth(),enabled=!account.busy)
  if(account.busy) MotionState("loading")
  if(account.error.isNotEmpty()) Text(account.error,color=MaterialTheme.colorScheme.error)
  if(account.notice.isNotEmpty()) Text(account.notice,color=Brand)
  Button(onClick={when(mode) {"register"->account.register(name,email,password);"reset"->account.reset(email);else->account.login(email,password)}},enabled=account.configured&&!account.busy,modifier=Modifier.fillMaxWidth()) {Text(when(mode){"register"->"Criar conta";"reset"->"Enviar instruções";else->"Entrar"})}
  TextButton(onClick={mode=if(mode=="register")"login" else "register";password=""},enabled=!account.busy) {Text(if(mode=="register")"Já tenho conta" else "Criar uma conta")}
  TextButton(onClick={mode=if(mode=="reset")"login" else "reset";password=""},enabled=!account.busy) {Text(if(mode=="reset")"Voltar ao login" else "Esqueci minha senha")}
  HorizontalDivider();OutlinedButton(onClick=account::preview,enabled=!account.busy,modifier=Modifier.fillMaxWidth()) {Text("Explorar demonstração local")}
 }
}
@Composable fun AccountScreen(account:AccountSession) {
 Page("Sua conta") {Hero(if(account.demo)"SIMULAÇÃO LOCAL" else "FIREBASE AUTH",account.name,account.email)
  Text(if(account.demo)"Esta sessão não acessa dados na nuvem." else "Sessão restaurada pelo Firebase. Cada usuário tem um namespace local independente.")
  if(!account.demo) OutlinedButton(account::verify,enabled=!account.busy) {Text("Enviar verificação de e-mail")}
  if(account.notice.isNotEmpty()) Text(account.notice);if(account.error.isNotEmpty()) Text(account.error,color=MaterialTheme.colorScheme.error)
  Text("Kotlin · Compose · Firebase Auth · Lottie",style=MaterialTheme.typography.titleMedium)
  Text("Pagamentos, saldos e pedidos são fictícios. A autenticação não transforma operações simuladas em transações reais.",style=MaterialTheme.typography.bodySmall)
  Button(account::logout,Modifier.fillMaxWidth()) {Text("Sair da conta")}
 }
}
@Composable fun Workspace(account:AccountSession,tabs:List<String>,content:@Composable (String,(String)->Unit)->Unit) {
 val nav=rememberNavController();val entry by nav.currentBackStackEntryAsState();val route=entry?.destination?.route ?: "home"
 fun go(target:String) {nav.navigate(target) {launchSingleTop=true;if(target in listOf("home","activity","tools","account")) {popUpTo("home") {saveState=true};restoreState=true}}}
 Scaffold(bottomBar={NavigationBar {tabs.forEachIndexed {index,label->val path=listOf("home","activity","tools","account")[index];NavigationBarItem(selected=route==path,onClick={go(path)},icon={Icon(listOf(Icons.Default.Dashboard,Icons.Default.ReceiptLong,Icons.Default.Tune,Icons.Default.Person)[index],label)},label={Text(label)})}}}) {padding->
  Box(Modifier.padding(padding)) {NavHost(nav,"home",enterTransition={fadeIn(tween(250))+slideInHorizontally {it/8}},exitTransition={fadeOut(tween(150))}) {
   listOf("home","activity","tools","account","operation","result","details").forEach {path->composable(path) {if(path=="account")AccountScreen(account) else content(path,::go)}}
  }}
 }
}
