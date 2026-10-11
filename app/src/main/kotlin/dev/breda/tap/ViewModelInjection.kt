package dev.breda.tap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel

/** A composition root owns adapter construction; ViewModels receive abstractions. */
class InjectedViewModelFactory<T : ViewModel>(private val create: () -> T) :
    ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <V : ViewModel> create(modelClass: Class<V>): V = create() as V
}

@Composable
fun accountViewModel(): AccountSession {
    val context = LocalContext.current.applicationContext
    val factory =
        remember(context) {
            InjectedViewModelFactory {
                AccountSession(FirebaseAuthentication(context), PreferenceSessionStore(context))
            }
        }
    return viewModel(factory = factory)
}

@Composable
fun tapViewModel(uid: String?): TapViewModel {
    val context = LocalContext.current.applicationContext
    val factory =
        remember(context, uid) {
            InjectedViewModelFactory {
                val storage =
                    PreferenceCheckoutStorage(
                        context.getSharedPreferences(
                            "checkout-${sessionNamespace(uid ?: "local-demo")}",
                            0,
                        )
                    )
                TapViewModel(
                    storage,
                    FirebaseHistory(context),
                    DemoCheckoutGateways(),
                    CheckoutRecovery(storage),
                )
            }
        }
    return viewModel(key = "tap-$uid", factory = factory)
}
