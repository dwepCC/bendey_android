package com.bendey.restaurant.core.data.printer.printserver

import android.content.Context
import android.os.Build
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrintServerClientHeaders @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val clientId: String by lazy {
        val prefs = context.getSharedPreferences("bendey_print_server", Context.MODE_PRIVATE)
        prefs.getString("client_id", null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString("client_id", it).apply()
        }
    }

    fun build(
        tenant: String = "",
        branchName: String = "",
        appVersion: String = "",
    ): Map<String, String> = mapOf(
        "X-Bendey-Client-Id" to clientId,
        "X-Bendey-Device-Name" to deviceName(),
        "X-Bendey-Device-Model" to (Build.MODEL ?: ""),
        "X-Bendey-Android-Version" to (Build.VERSION.RELEASE ?: ""),
        "X-Bendey-App-Version" to appVersion,
        "X-Bendey-Tenant" to tenant,
        "X-Bendey-Branch" to branchName,
    )

    private fun deviceName(): String {
        return Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
            ?.takeIf { it.isNotBlank() }
            ?: Build.MODEL
            ?: "Android"
    }
}
