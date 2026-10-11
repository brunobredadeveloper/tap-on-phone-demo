package dev.breda.tap

import android.content.SharedPreferences
import dev.breda.payments.PaymentRequest
import org.json.JSONArray
import org.json.JSONObject

class PreferenceCheckoutStorage(private val preferences: SharedPreferences) : CheckoutStorage {
    override var connection: CheckoutConnection
        get() =
            CheckoutConnection(
                preferences.getString("endpoint", "http://10.0.2.2:8080")!!,
                preferences.getBoolean("useBackend", false),
            )
        set(value) {
            preferences
                .edit()
                .putString("endpoint", value.endpoint)
                .putBoolean("useBackend", value.backend)
                .apply()
        }

    override var pending: PendingCheckout?
        get() {
            val key = preferences.getString("pendingKey", null) ?: return null
            val amount = preferences.getLong("pendingAmount", 0)
            if (amount <= 0) return null
            return PendingCheckout(
                PaymentRequest(amount, key),
                preferences.getBoolean("pendingBackend", true),
                preferences.getString("pendingScenario", "approve")!!,
                preferences.getString("pendingEndpoint", connection.endpoint)!!,
            )
        }
        set(value) {
            val editor = preferences.edit()
            if (value == null) editor.remove("pendingKey").remove("pendingAmount")
            else
                editor
                    .putString("pendingKey", value.request.idempotencyKey)
                    .putLong("pendingAmount", value.request.amountInCents)
                    .putBoolean("pendingBackend", value.backend)
                    .putString("pendingScenario", value.scenario)
                    .putString("pendingEndpoint", value.endpoint)
            check(editor.commit()) { "Não foi possível salvar a cobrança pendente." }
        }

    override var sales: List<Sale>
        get() =
            runCatching {
                    val rows = JSONArray(preferences.getString("sales", "[]"))
                    (0 until rows.length()).map { index ->
                        val row = rows.getJSONObject(index)
                        Sale(
                            row.getString("key"),
                            row.getLong("amount"),
                            row.getString("status"),
                            row.optString("transactionId"),
                            row.getLong("at"),
                        )
                    }
                }
                .getOrDefault(emptyList())
        set(value) {
            val rows = JSONArray()
            value.forEach { sale ->
                rows.put(
                    JSONObject()
                        .put("key", sale.key)
                        .put("amount", sale.amount)
                        .put("status", sale.status)
                        .put("transactionId", sale.transactionId)
                        .put("at", sale.at)
                )
            }
            check(preferences.edit().putString("sales", rows.toString()).commit()) {
                "Não foi possível salvar o comprovante. Consulte a cobrança."
            }
        }
}
