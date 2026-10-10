package com.bendey.restaurant

import android.Manifest
import android.content.Intent
import android.content.res.Configuration
import com.bendey.restaurant.core.navigation.DeliveryDeepLink
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.bendey.restaurant.push.PushRegistrar
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.bendey.restaurant.core.data.session.SessionExpiryCoordinator
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyTheme
import com.bendey.restaurant.core.ui.layout.BendeyOrientationPolicy
import com.bendey.restaurant.navigation.BendeyAppNavHost
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var sessionExpiryCoordinator: SessionExpiryCoordinator

    @Inject
    lateinit var pushRegistrar: PushRegistrar

    override fun onCreate(savedInstanceState: Bundle?) {
        applyOrientationPolicy()
        super.onCreate(savedInstanceState)
        handlePushIntent(intent)
        // Android 13+: sin este permiso no se muestra ningun aviso de pedido. FCM: registra el token al haber sesion.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
                .launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        lifecycleScope.launch { pushRegistrar.registerWhileLoggedIn() }
        val systemTomato = BendeyColors.Rest900.toArgb()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(systemTomato),
            navigationBarStyle = SystemBarStyle.dark(systemTomato),
        )
        setContent {
            BendeyTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()
                val context = LocalContext.current

                // Expiración de sesión global: al recibir 401 se cierra sesión y se redirige al
                // login (vía isAuthenticatedFlow); aquí solo avisamos con un Toast, visible incluso
                // durante la transición al login (el snackbar vive dentro del shell autenticado).
                LaunchedEffect(Unit) {
                    sessionExpiryCoordinator.events.collect { message ->
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    }
                }

                BendeyAppNavHost(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHostState = snackbarHostState,
                    onShowMessage = { message ->
                        scope.launch { snackbarHostState.showSnackbar(message) }
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePushIntent(intent)
    }

    /** Tocar un aviso push abre la pantalla que corresponde a su data.type (hoy: payment_review -> Entregas > Pagos). */
    private fun handlePushIntent(intent: Intent?) {
        DeliveryDeepLink.handlePushType(intent?.getStringExtra("type"))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyOrientationPolicy()
    }

    /** Telefonos: vertical. Tablets: libre, y horizontal fijo solo mientras esta la pantalla de cocina (KDS). */
    private fun applyOrientationPolicy() = BendeyOrientationPolicy.apply(this)
}
