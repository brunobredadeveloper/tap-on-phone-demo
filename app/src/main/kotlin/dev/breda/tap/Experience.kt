package dev.breda.tap

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import com.airbnb.lottie.compose.*
import kotlinx.coroutines.delay

val Brand = Color(0xFFFFBB54)
val BrandTitle = "Breda Tap"

@Composable
fun BrandTheme(content: @Composable () -> Unit) {
    val scheme =
        darkColorScheme(
            primary = Brand,
            secondary = Color(0xFF6EF5D0),
            background = Color(0xFF161310),
            surface = Color(0xFF242018),
        )
    MaterialTheme(
        colorScheme = scheme,
        shapes =
            Shapes(
                small = RoundedCornerShape(14.dp),
                medium = RoundedCornerShape(24.dp),
                large = RoundedCornerShape(32.dp),
            ),
        content = content,
    )
}

@Composable
fun BrandMark(size: Dp = 64.dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size / 4)).background(Color(0xFF101629)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painterResource(R.drawable.ic_brand),
            "Logo breda_dev",
            Modifier.fillMaxSize().padding(size / 10),
        )
    }
}

object MotionPolicy {
    var freezeForTests = false
}

@Composable
fun MotionState(kind: String, modifier: Modifier = Modifier, size: Dp = 240.dp) {
    val result = rememberLottieComposition(LottieCompositionSpec.Asset("motion_$kind.json"))
    val composition = result.value
    val animatedProgress by
        animateLottieCompositionAsState(
            composition,
            isPlaying = !MotionPolicy.freezeForTests,
            iterations =
                if (kind in listOf("loading", "splash")) LottieConstants.IterateForever else 1,
            restartOnPlay = true,
        )
    val progress = if (MotionPolicy.freezeForTests) .6f else animatedProgress
    Box(modifier.size(size).testTag("motion-$kind"), contentAlignment = Alignment.Center) {
        if (composition != null)
            LottieAnimation(
                composition,
                { progress },
                Modifier.fillMaxSize().testTag("lottie-$kind"),
            )
        else if (kind in listOf("loading", "splash"))
            CircularProgressIndicator(Modifier.size(size / 3), color = Brand, strokeWidth = 5.dp)
        else
            Icon(
                if (kind == "success") Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                kind,
                Modifier.size(size / 2),
                tint = if (kind == "success") Color(0xFF12B886) else MaterialTheme.colorScheme.error,
            )
    }
}

@Composable
fun Hero(eyebrow: String, title: String, subtitle: String) {
    val glow = rememberInfiniteTransition(label = "ambient")
    val alpha by
        glow.animateFloat(
            .08f,
            .18f,
            infiniteRepeatable(tween(3000), RepeatMode.Reverse),
            label = "ambient-alpha",
        )
    Column(
        Modifier.fillMaxWidth()
            .background(
                Brush.linearGradient(
                    listOf(Brand.copy(alpha = alpha), MaterialTheme.colorScheme.surface)
                ),
                RoundedCornerShape(28.dp),
            )
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            eyebrow.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = Brand,
            fontWeight = FontWeight.Bold,
        )
        Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun ActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector = Icons.Default.ArrowForward,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                Modifier.size(48.dp)
                    .background(Brand.copy(alpha = .10f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = Brand)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun Page(title: String, back: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        if (back != null)
            TextButton(onClick = back) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                Text("Voltar")
            }
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        content()
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun StatusPanel(kind: String, title: String, description: String) {
    Surface(
        Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MotionState(kind, size = 164.dp)
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                description,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Dedicated, full-page transaction feedback. No bottom navigation while processing. */
@Composable
fun ResultExperience(
    kind: String,
    title: String,
    description: String,
    amount: String = "",
    reference: String = "",
    primaryLabel: String = "",
    onPrimary: (() -> Unit)? = null,
    secondaryLabel: String = "",
    onSecondary: (() -> Unit)? = null,
    extra: @Composable ColumnScope.() -> Unit = {},
) {
    val tint =
        when (kind) {
            "success" ->
                if (MaterialTheme.colorScheme.background.luminance() > .5f) Color(0xFF07865E)
                else Color(0xFF12B886)
            "error" -> Color(0xFFF05D6C)
            else -> Brand
        }
    BackHandler {
        if (kind != "loading") {
            onSecondary?.invoke() ?: onPrimary?.invoke()
        }
    }
    Column(
        Modifier.fillMaxSize()
            .testTag("result-$kind")
            .background(
                Brush.verticalGradient(
                    listOf(tint.copy(alpha = .12f), MaterialTheme.colorScheme.background)
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            BrandTitle.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        key(kind) { MotionState(kind, size = 260.dp) }
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        if (amount.isNotBlank())
            Text(
                amount,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = tint,
            )
        Text(
            description,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (kind == "loading") {
            LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 40.dp))
            Text("Aguarde o resultado para continuar.", style = MaterialTheme.typography.bodySmall)
        } else {
            if (reference.isNotBlank())
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text(
                            "REFERÊNCIA",
                            style = MaterialTheme.typography.labelSmall,
                            color = tint,
                        )
                        Text(reference, style = MaterialTheme.typography.bodySmall)
                    }
                }
            if (onPrimary != null)
                Button(
                    onClick = onPrimary,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                ) {
                    Text(primaryLabel)
                }
            if (onSecondary != null)
                OutlinedButton(
                    onClick = onSecondary,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                ) {
                    Text(secondaryLabel)
                }
        }
        extra()
        Spacer(Modifier.height(16.dp))
        Text(
            "breda_dev",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LaunchExperience() {
    Box(
        Modifier.fillMaxSize()
            .testTag("splash")
            .background(
                Brush.verticalGradient(
                    listOf(Brand.copy(alpha = .13f), MaterialTheme.colorScheme.background)
                )
            )
    ) {
        Column(
            Modifier.fillMaxSize().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            BrandMark(72.dp)
            Spacer(Modifier.height(12.dp))
            MotionState("splash", size = 220.dp)
            Text(
                BrandTitle,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Uma venda. Uma experiência.",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Preparando sua experiência",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            "breda_dev  /  feito para evoluir",
            Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun ExperienceRoot(
    account: AccountSession = accountViewModel(),
    content: @Composable (AccountSession) -> Unit,
) {
    var splash by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(3200)
        splash = false
    }
    Surface(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
        color = MaterialTheme.colorScheme.background,
    ) {
        AnimatedContent(
            when {
                splash -> "splash"
                !account.onboarded -> "intro"
                account.session == null -> "auth"
                else -> "app"
            },
            transitionSpec = { fadeIn(tween(450)) togetherWith fadeOut(tween(250)) },
            label = "launch-flow",
        ) { screen ->
            when (screen) {
                "splash" -> LaunchExperience()
                "intro" ->
                    Page("Uma venda. Uma experiência.") {
                        BrandMark()
                        Hero(
                            "TERMINAL DO LOJISTA",
                            BrandTitle,
                            "Prepare, aproxime e acompanhe o resultado.",
                        )
                        Text(
                            "Uma jornada de venda com retorno claro em cada etapa.",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            "Cobranças, vendas e conciliação",
                            color = Brand,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Button(
                            account::finishOnboarding,
                            Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        ) {
                            Text("Começar")
                        }
                        Text(
                            "Operações fictícias para explorar com tranquilidade.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                "auth" -> AuthScreen(account)
                else -> key(account.session) { content(account) }
            }
        }
    }
}

@Composable
private fun AuthScreen(account: AccountSession) {
    var mode by rememberSaveable { mutableStateOf("login") }
    var email by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    BackHandler(enabled = mode != "login") {
        mode = "login"
        password = ""
    }
    Column(
        Modifier.fillMaxSize().testTag("auth").verticalScroll(rememberScrollState()).padding(26.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            BrandMark(54.dp)
            Column {
                Text(
                    BrandTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text("by breda_dev", style = MaterialTheme.typography.labelSmall, color = Brand)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            when (mode) {
                "register" -> "Seu próximo passo começa aqui."
                "reset" -> "Vamos recuperar seu acesso."
                else -> "Bom ter você por aqui."
            },
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            when (mode) {
                "register" -> "Crie uma conta para organizar sua experiência."
                "reset" -> "Informe seu e-mail e enviaremos as instruções."
                else -> "Prepare, aproxime e acompanhe o resultado."
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!account.configured)
            Surface(color = Brand.copy(alpha = .08f), shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Info, null, tint = Brand)
                    Text(
                        "O login estará disponível após conectar seu Firebase. Você já pode explorar o app no modo local.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        if (mode == "register")
            OutlinedTextField(
                name,
                { name = it },
                label = { Text("Seu nome") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !account.busy,
                leadingIcon = { Icon(Icons.Default.PersonOutline, null) },
            )
        OutlinedTextField(
            email,
            { email = it },
            label = { Text("E-mail") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            enabled = !account.busy,
            leadingIcon = { Icon(Icons.Default.AlternateEmail, null) },
        )
        if (mode != "reset")
            OutlinedTextField(
                password,
                { password = it },
                label = { Text("Senha") },
                visualTransformation =
                    if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                enabled = !account.busy,
                leadingIcon = { Icon(Icons.Default.Lock, null) },
                trailingIcon = {
                    IconButton(onClick = { visible = !visible }) {
                        Icon(
                            if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            if (visible) "Ocultar senha" else "Mostrar senha",
                        )
                    }
                },
            )
        if (account.busy)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                MotionState("loading", size = 120.dp)
            }
        if (account.error.isNotEmpty()) Text(account.error, color = MaterialTheme.colorScheme.error)
        if (account.notice.isNotEmpty()) Text(account.notice, color = Brand)
        Button(
            onClick = {
                when (mode) {
                    "register" -> account.register(name, email, password)
                    "reset" -> account.reset(email)
                    else -> account.login(email, password)
                }
            },
            enabled = account.configured && !account.busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        ) {
            Text(
                when (mode) {
                    "register" -> "Criar minha conta"
                    "reset" -> "Enviar instruções"
                    else -> "Entrar"
                }
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(
                onClick = {
                    mode = if (mode == "register") "login" else "register"
                    password = ""
                },
                enabled = !account.busy,
            ) {
                Text(if (mode == "register") "Já tenho conta" else "Criar conta")
            }
            TextButton(
                onClick = {
                    mode = if (mode == "reset") "login" else "reset"
                    password = ""
                },
                enabled = !account.busy,
            ) {
                Text(if (mode == "reset") "Voltar" else "Esqueci a senha")
            }
        }
        HorizontalDivider()
        OutlinedButton(
            onClick = account::preview,
            enabled = !account.busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        ) {
            Text("Explorar demonstração local")
        }
    }
}

@Composable
fun AccountScreen(account: AccountSession) {
    Page("Sua conta") {
        BrandMark()
        Hero(if (account.demo) "MODO LOCAL" else "MINHA CONTA", account.name, account.email)
        Text(
            if (account.demo) "Você está explorando dados fictícios salvos neste aparelho."
            else "Sua sessão está protegida pelo Firebase Authentication."
        )
        if (!account.demo)
            OutlinedButton(account::verify, enabled = !account.busy) {
                Text("Enviar verificação de e-mail")
            }
        if (account.busy) MotionState("loading", size = 120.dp)
        if (account.notice.isNotEmpty()) Text(account.notice)
        if (account.error.isNotEmpty()) Text(account.error, color = MaterialTheme.colorScheme.error)
        Text("Criado por breda_dev", style = MaterialTheme.typography.titleMedium)
        Text(
            "Uma experiência de demonstração. Nenhuma operação movimenta dinheiro real.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(account::logout, Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Text("Sair da conta")
        }
    }
}

@Composable
fun Workspace(
    account: AccountSession,
    tabs: List<String>,
    content: @Composable (String, (String) -> Unit) -> Unit,
) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: "home"
    val roots = listOf("home", "activity", "tools", "account")
    fun go(target: String) {
        if (target == route) return
        if (target == "home") {
            nav.popBackStack("home", false)
            return
        }
        if (target in roots) {
            if (!nav.popBackStack(target, false))
                nav.navigate(target) {
                    popUpTo("home") { inclusive = false }
                    launchSingleTop = true
                }
        } else if (target == "operation" && route == "result") {
            if (!nav.popBackStack("operation", false))
                nav.navigate(target) { launchSingleTop = true }
        } else nav.navigate(target) { launchSingleTop = true }
    }
    val fullScreen = route in listOf("result", "compatibility", "operation")
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (!fullScreen)
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    tabs.forEachIndexed { index, label ->
                        val path = roots[index]
                        NavigationBarItem(
                            selected = route == path,
                            onClick = { go(path) },
                            icon = {
                                Icon(
                                    listOf(
                                        Icons.Default.Dashboard,
                                        Icons.Default.ReceiptLong,
                                        Icons.Default.Tune,
                                        Icons.Default.Person,
                                    )[index],
                                    null,
                                )
                            },
                            label = { Text(label) },
                            alwaysShowLabel = true,
                        )
                    }
                }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            NavHost(
                nav,
                "home",
                enterTransition = { fadeIn(tween(350)) + slideInHorizontally { it / 6 } },
                exitTransition = { fadeOut(tween(180)) },
                popEnterTransition = { fadeIn(tween(250)) },
                popExitTransition = { fadeOut(tween(180)) },
            ) {
                (roots + listOf("operation", "result", "details", "compatibility")).forEach { path
                    ->
                    composable(path) {
                        if (path == "account") AccountScreen(account) else content(path, ::go)
                    }
                }
            }
        }
    }
}
