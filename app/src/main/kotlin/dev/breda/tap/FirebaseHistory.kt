package dev.breda.tap
import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

/** Optional configuration; no credentials or cloud project are fabricated. */
class FirebaseHistory(context: Context) {
 private val app: FirebaseApp? = try {
  val c=JSONObject(context.assets.open("firebase-config.json").bufferedReader().use { it.readText() })
  FirebaseApp.getApps(context).firstOrNull { it.name == "tap-demo" } ?: FirebaseApp.initializeApp(context,
   FirebaseOptions.Builder().setProjectId(c.getString("projectId")).setApplicationId(c.getString("applicationId")).setApiKey(c.getString("apiKey")).build(),"tap-demo")
 } catch (_: java.io.FileNotFoundException) { null }
 val configured get()=app!=null
 private suspend fun uid(): String {
  val auth=FirebaseAuth.getInstance(requireNotNull(app));return auth.currentUser?.uid ?: requireNotNull(auth.signInAnonymously().await().user).uid
 }
 suspend fun save(key: String, amount: Long, status: String) {
  val user=uid();FirebaseFirestore.getInstance(requireNotNull(app)).collection("users").document(user).collection("demoPayments").document(key)
   .set(mapOf("amountInCents" to amount,"status" to status,"simulation" to true)).await()
 }
 suspend fun history(): List<String> {
  val user=uid();return FirebaseFirestore.getInstance(requireNotNull(app)).collection("users").document(user).collection("demoPayments").get().await().documents
   .map { "${it.getLong("amountInCents")} centavos · ${it.getString("status")}" }
 }
}
