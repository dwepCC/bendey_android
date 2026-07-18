package com.bendey.restaurant

import android.content.pm.ActivityInfo
import android.content.res.Configuration
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
import com.bendey.restaurant.core.ui.layout.BendeyDeviceFormFactor
import com.bendey.restaurant.navigation.BendeyAppNavHost
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var sessionExpiryCoordinator: SessionExpiryCoordinator

    override fun onCreate(savedInstanceState: Bundle?) {
        applyOrientationPolicy()
        super.onCreate(savedInstanceState)
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

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyOrientationPolicy()
    }

    /**
     * Teléfonos: solo portrait.
     * Tablets (smallestScreenWidthDp ≥ 600): portrait y landscape.
     */
    private fun applyOrientationPolicy() {
        val smallestWidth = resources.configuration.smallestScreenWidthDp
        requestedOrientation = if (BendeyDeviceFormFactor.isTablet(smallestWidth)) {
            ActivityInfo.SCREEN_ORIENTATION_FULL_USER
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }
}
