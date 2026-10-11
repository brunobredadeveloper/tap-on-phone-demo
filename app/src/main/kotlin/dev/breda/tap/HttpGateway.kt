package dev.breda.tap
import dev.breda.payments.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI

class HttpGateway(private val endpoint: String, private val scenario: String) : PaymentGateway {
 init { val uri=URI(endpoint);require(uri.scheme in listOf("http","https") && uri.host!=null && uri.userInfo==null && uri.query==null && uri.fragment==null) { "URL inválida" } }
 private suspend fun call(path: String, payload: JSONObject? = null): Pair<Int,JSONObject> = withContext(Dispatchers.IO) {
  val connection=URI(endpoint.trimEnd('/')+path).toURL().openConnection() as HttpURLConnection
  connection.connectTimeout=2000;connection.readTimeout=2000
  try {
   if(payload!=null){connection.requestMethod="POST";connection.doOutput=true;connection.setRequestProperty("Content-Type","application/json");connection.outputStream.use { it.write(payload.toString().toByteArray()) }}
   val code=connection.responseCode;val stream=if(code>=400) connection.errorStream else connection.inputStream
   code to JSONObject(stream?.bufferedReader()?.use { it.readText() } ?: "{}")
  } finally { connection.disconnect() }
 }
 override suspend fun authorize(request: PaymentRequest): PaymentResult {
  val (code,json)=call("/api/payments",JSONObject().put("idempotencyKey",request.idempotencyKey).put("amountInCents",request.amountInCents).put("scenario",scenario))
  return decode(code,json)
 }
 suspend fun reconcile(key: String): PaymentResult { val (code,json)=call("/api/payments/by-key/$key");return decode(code,json) }
 private fun decode(code: Int,json: JSONObject): PaymentResult = when {
  code==409 -> PaymentResult.IdempotencyConflict
  code !in 200..299 -> PaymentResult.OutcomeUnknown
  json.optString("status")=="approved" -> PaymentResult.Approved(Receipt(json.getString("id"),json.getLong("amount")))
  json.optString("status")=="declined" -> PaymentResult.Declined(DeclineReason.REJECTED_BY_ISSUER)
  else -> PaymentResult.OutcomeUnknown
 }
}
