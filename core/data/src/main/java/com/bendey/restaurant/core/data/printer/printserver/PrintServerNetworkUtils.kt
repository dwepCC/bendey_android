package com.bendey.restaurant.core.data.printer.printserver

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.os.Build
import java.net.Inet4Address
import java.net.NetworkInterface

/** IPv4 privada de la Wi-Fi/LAN activa del dispositivo. */
fun deviceLanIpv4(context: Context): String? {
    val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    if (cm != null) {
        val active = cm.activeNetwork
        if (active != null) {
            pickPrivateIpv4(cm.getLinkProperties(active))?.let { return it }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (net in cm.allNetworks) {
                pickPrivateIpv4(cm.getLinkProperties(net))?.let { return it }
            }
        }
    }

    val interfaces = NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
    for (ni in interfaces) {
        if (!ni.isUp || ni.isLoopback) continue
        for (addr in ni.inetAddresses) {
            if (addr is Inet4Address && !addr.isLoopbackAddress) {
                val host = addr.hostAddress?.trim().orEmpty()
                if (isPrivateLanIpv4(host)) return host
            }
        }
    }
    return null
}

private fun pickPrivateIpv4(props: LinkProperties?): String? {
    if (props == null) return null
    for (link in props.linkAddresses) {
        val address = link.address
        if (address is Inet4Address && !address.isLoopbackAddress) {
            val host = address.hostAddress?.trim().orEmpty()
            if (isPrivateLanIpv4(host)) return host
        }
    }
    return null
}

fun isPrivateLanIpv4(host: String): Boolean {
    if (host.isBlank() || host == "127.0.0.1") return false
    val parts = host.split('.')
    if (parts.size != 4) return false
    val a = parts[0].toIntOrNull() ?: return false
    val b = parts[1].toIntOrNull() ?: return false
    return when {
        a == 10 -> true
        a == 172 && b in 16..31 -> true
        a == 192 && b == 168 -> true
        a == 169 && b == 254 -> true
        else -> false
    }
}

fun subnetPrefixFromIp(ip: String): String? {
    val parts = ip.split('.')
    if (parts.size != 4) return null
    return "${parts[0]}.${parts[1]}.${parts[2]}."
}
