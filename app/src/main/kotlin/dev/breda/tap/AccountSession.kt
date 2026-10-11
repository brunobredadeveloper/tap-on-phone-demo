package dev.breda.tap
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.*
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.security.MessageDigest

fun sessionNamespace(uid:String):String { require(uid.isNotBlank());return MessageDigest.getInstance("SHA-256").digest(uid.toByteArray()).joinToString("") { "%02x".format(it) } }
class AccountSession(application:Application):AndroidViewModel(application) {
 private val preferences=application.getSharedPreferences("portfolio-account",0)
 private val firebase:FirebaseApp?=try {
  val c=JSONObject(application.assets.open("firebase-config.json").bufferedReader().use { it.readText() })
  FirebaseApp.getApps(application).firstOrNull { it.name=="tap-demo" } ?: FirebaseApp.initializeApp(application,FirebaseOptions.Builder().setProjectId(c.getString("projectId")).setApplicationId(c.getString("applicationId")).setApiKey(c.getString("apiKey")).build(),"tap-demo")
 }catch(_:java.io.FileNotFoundException) { null }catch(_:Exception) { null }
 private val auth=firebase?.let { FirebaseAuth.getInstance(it) }
 val configured get()=auth!=null
 var session by mutableStateOf<String?>(null);private set
 var email by mutableStateOf("");private set
 var name by mutableStateOf("");private set
 var busy by mutableStateOf(false);private set
 var error by mutableStateOf("");private set
 var notice by mutableStateOf("");private set
 var onboarded by mutableStateOf(preferences.getBoolean("onboarded",false));private set
 val demo get()=session=="local-demo"
 private val listener=FirebaseAuth.AuthStateListener { value ->
  val user=value.currentUser
  if(user!=null && !user.isAnonymous) {
   preferences.edit().putString("namespace",sessionNamespace(user.uid)).commit()
   email=user.email.orEmpty();name=user.displayName ?: "Bruno";session=user.uid
  }else if(!demo) {session=null;email="";name=""}
 }
 init {auth?.setLanguageCode("pt");auth?.addAuthStateListener(listener)}
 fun finishOnboarding() {preferences.edit().putBoolean("onboarded",true).apply();onboarded=true}
 fun preview() {auth?.signOut();preferences.edit().putString("namespace",sessionNamespace("local-demo")).commit();session="local-demo";name="Bruno · demonstração";email="Ambiente local";error=""}
 fun login(email:String,password:String) = perform {require(email.contains('@') && password.isNotBlank()) {"Informe e-mail e senha."};requireNotNull(auth) {"Configure o Firebase para entrar."}.signInWithEmailAndPassword(email.trim(),password).await()}
 fun register(name:String,email:String,password:String) = perform {
  require(name.trim().length>=2 && email.contains('@') && password.length>=8) {"Use nome, e-mail válido e senha com pelo menos 8 caracteres."}
  val result=requireNotNull(auth) {"Firebase não configurado."}.createUserWithEmailAndPassword(email.trim(),password).await()
  result.user?.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name.trim()).build())?.await();this@AccountSession.name=name.trim()
  notice="Conta criada."
 }
 fun reset(email:String) = perform {require(email.contains('@')) {"Informe seu e-mail."};requireNotNull(auth) {"Firebase não configurado."}.sendPasswordResetEmail(email.trim()).await();notice="Se existir uma conta para esse e-mail, você receberá as instruções."}
 fun verify() = perform {requireNotNull(auth?.currentUser).sendEmailVerification().await();notice="E-mail de verificação enviado."}
 fun logout() {auth?.signOut();preferences.edit().putString("namespace",sessionNamespace("local-demo")).commit();session=null;name="";email="";error="";notice=""}
 private fun perform(block:suspend ()->Unit) {if(busy)return;busy=true;error="";notice="";viewModelScope.launch {try{block()}catch(e:CancellationException){throw e}catch(e:Exception){error=when(e){is com.google.firebase.FirebaseNetworkException->"Sem conexão. Confira sua rede e tente novamente.";is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException,is com.google.firebase.auth.FirebaseAuthInvalidUserException->"Não foi possível autenticar. Confira e-mail e senha.";is com.google.firebase.auth.FirebaseAuthUserCollisionException->"Esse e-mail já possui uma conta.";else->e.localizedMessage ?: "Não foi possível concluir."}}finally{busy=false}}}
 override fun onCleared() {auth?.removeAuthStateListener(listener)}
}
