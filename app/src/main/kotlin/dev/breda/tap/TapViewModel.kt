package dev.breda.tap

import androidx.compose.runtime.*
import androidx.lifecycle.viewModelScope
import dev.breda.payments.*
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

data class Sale(
    val key: String,
    val amount: Long,
    val status: String,
    val transactionId: String,
    val at: Long = System.currentTimeMillis(),
)

class TapViewModel(
    private val storage: CheckoutStorage,
    private val cloud: SaleHistory,
    private val gateways: CheckoutGatewayFactory,
    private val recovery: CheckoutRecovery,
) : androidx.lifecycle.ViewModel() {
    var state by mutableStateOf<CheckoutState>(CheckoutState.Idle)
        private set

    var message by mutableStateOf("Pronto para uma venda fictícia")
        private set

    var history by mutableStateOf(listOf<String>())
        private set

    val firebaseEnabled
        get() = cloud.configured

    private var client: PaymentClient? = null
    private var selectedScenario = "approve"
    private var selectedBackend = false
    private var checkoutEndpoint = ""
    var sales by mutableStateOf(listOf<Sale>())
        private set

    var consulting by mutableStateOf(false)
        private set

    var endpoint by mutableStateOf(storage.connection.endpoint)
    var useBackend by mutableStateOf(storage.connection.backend)

    fun saveConnection() {
        storage.connection = CheckoutConnection(endpoint, useBackend)
    }

    init {
        sales = storage.sales
        history = sales.map { "${it.amount} centavos · ${it.status}" }
        storage.pending?.let { pending ->
            state = CheckoutState.Completed(pending.request, PaymentResult.OutcomeUnknown)
            message = "Cobrança pendente recuperada. Consulte o backend antes de uma nova cobrança."
        }
    }

    fun beginCheckout(input: String, backend: Boolean, url: String, scenario: String) {
        if (state is CheckoutState.Waiting || state is CheckoutState.Processing || consulting)
            return
        if ((storage.pending != null)) {
            message = "Concilie a cobrança pendente primeiro."
            return
        }
        val amount =
            AmountInput.cents(input)
                ?: run {
                    message = "Informe um valor válido, com até duas casas decimais"
                    return
                }
        try {
            selectedScenario = scenario
            selectedBackend = backend
            checkoutEndpoint = url
            client = PaymentClient(gateways.create(backend, url, scenario))
            val request = PaymentRequest(amount, UUID.randomUUID().toString())
            state = begin(state, request)
            message = "Aguardando aproximação simulada"
        } catch (e: Exception) {
            message = e.message ?: "Configuração inválida"
        }
    }

    fun simulateTap() {
        val waiting = state as? CheckoutState.Waiting ?: return
        try {
            recovery.start(
                PendingCheckout(
                    waiting.request,
                    selectedBackend,
                    selectedScenario,
                    checkoutEndpoint,
                )
            )
        } catch (failure: Exception) {
            message = failure.message ?: "Não foi possível iniciar a cobrança."
            return
        }
        state = tap(state)
        viewModelScope.launch {
            try {
                kotlinx.coroutines.delay(1400)
                val result = requireNotNull(client).pay(waiting.request)
                state = finish(state, waiting.request.idempotencyKey, result)
                record(waiting.request, result)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                state = CheckoutState.Completed(waiting.request, PaymentResult.OutcomeUnknown)
                message = "Resultado incerto. Consulte a cobrança antes de tentar novamente."
            }
        }
    }

    fun reconcile(url: String) {
        val completed = state as? CheckoutState.Completed ?: return
        if (consulting) return
        consulting = true
        viewModelScope.launch {
            try {
                val pending = storage.pending ?: return@launch
                val result = gateways.reconcile(pending)
                state = CheckoutState.Completed(completed.request, result)
                record(completed.request, result)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                message = "Consulta falhou: ${e.message}"
            } finally {
                consulting = false
            }
        }
    }

    private suspend fun record(request: PaymentRequest, result: PaymentResult) {
        val status =
            when (result) {
                is PaymentResult.Approved -> "approved"
                is PaymentResult.Declined -> "declined"
                else -> "unknown"
            }
        message =
            when (result) {
                is PaymentResult.Approved -> "Aprovado · ${result.receipt.transactionId}"
                is PaymentResult.Declined -> "Recusado"
                else -> "Resultado incerto: consulte a cobrança"
            }
        recovery.record(request, result)
        sales = storage.sales
        history = sales.map { "${it.amount} centavos · ${it.status}" }
        if (cloud.configured)
            try {
                cloud.save(request.idempotencyKey, request.amountInCents, status)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                message += " · Histórico Firebase pendente: ${e.message}"
            }
    }

    fun prepareNew() {
        if (!(storage.pending != null) && state is CheckoutState.Completed)
            state = CheckoutState.Idle
    }

    fun cancelWaiting() {
        if (state is CheckoutState.Waiting) {
            state = CheckoutState.Idle
            message = "Cobrança cancelada antes da aproximação."
        }
    }

    fun refreshHistory() {
        if (!cloud.configured) return
        viewModelScope.launch {
            try {
                history = cloud.history()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                message = "Firebase: ${e.message}"
            }
        }
    }
}
