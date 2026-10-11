package dev.breda.tap
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.UiDevice
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ExperienceJourneyTest {
 @get:Rule val ui=createEmptyComposeRule()
 private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
 private var scenario:ActivityScenario<MainActivity>?=null
 private fun capture(name:String){val dir=File(context.getExternalFilesDir(null),"ux");dir.mkdirs();UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).takeScreenshot(File(dir,"$name.png"))}
 private fun click(text:String){val node=ui.onNodeWithText(text);runCatching{node.assertIsDisplayed()}.onFailure{node.performScrollTo()};node.performClick()}
 private fun shown(tag:String){ui.waitUntil(15000){ui.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()};ui.onNodeWithTag(tag).assertIsDisplayed()}
 @Before fun prepare(){context.getSharedPreferences("portfolio-account",0).edit().clear().commit();MotionPolicy.freezeForTests=true}
 @After fun finish(){scenario?.close();MotionPolicy.freezeForTests=false}
 @Test fun splashLoginNavigationAndTransactionFeedback(){
  ui.mainClock.autoAdvance=false
  scenario=ActivityScenario.launch(MainActivity::class.java)
  ui.mainClock.advanceTimeByFrame();shown("splash");capture("01-splash")
  ui.mainClock.advanceTimeBy(3600);ui.mainClock.autoAdvance=true
  ui.waitUntil(15000){ui.onAllNodesWithText("Começar").fetchSemanticsNodes().isNotEmpty()};click("Começar")
  shown("auth");capture("02-login")
  click("Criar conta");ui.onNodeWithText("Seu nome").assertExists();click("Já tenho conta")
  click("Esqueci a senha");ui.onNodeWithText("Enviar instruções").assertExists();click("Voltar")
  click("Explorar demonstração local")
  ui.waitUntil(15000){ui.onAllNodesWithTag("device-compatibility").fetchSemanticsNodes().isNotEmpty()};capture("00-nfc-feedback");click("Usar somente a simulação")
  capture("03-home")
  click("Nova cobrança");click("Preparar cobrança");click("Simular aproximação");shown("result-loading");capture("04-loading");shown("result-success");capture("05-success");click("Ir para o terminal");click("Nova cobrança");click("Recusa");click("Preparar cobrança");click("Simular aproximação");shown("result-loading");shown("result-error");capture("06-error");click("Ir para o terminal");click("Vendas");ui.onNodeWithText("Buscar referência").assertExists();capture("07-history");
  click("Conta");ui.onNodeWithText("Sua conta").assertExists();capture("08-account");click("Sair da conta");shown("auth")
 }
 @Test fun lottieAssetsActuallyDrawAndChangeFrames(){
  for(kind in listOf("splash","loading","success","error")){
   val parsed=LottieCompositionFactory.fromAssetSync(context,"motion_$kind.json")
   assertNull("$kind must parse",parsed.exception);val composition=requireNotNull(parsed.value)
   assertTrue(composition.layers.isNotEmpty())
   val drawable=LottieDrawable();drawable.setComposition(composition);drawable.setBounds(0,0,320,320)
   fun frame(progress:Float):IntArray{val bitmap=Bitmap.createBitmap(320,320,Bitmap.Config.ARGB_8888);drawable.progress=progress;drawable.draw(Canvas(bitmap));return IntArray(320*320).also{bitmap.getPixels(it,0,320,0,0,320,320);bitmap.recycle()}}
   val first=frame(.08f);val second=frame(.65f)
   assertTrue("$kind must be visible",second.count{it ushr 24!=0}>250)
   assertFalse("$kind must change frames",first.contentEquals(second))
  }
 }
}
