package com.mywifiadata.app.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.ScanResult
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.TelephonyManager
import com.mywifiadata.app.data.model.WifiBand
import com.mywifiadata.app.data.model.WifiStatus
import java.net.Inet4Address

/**
 * قراءة حالة شبكة الواي فاي الحالية: الاسم، قوة الإشارة، سرعة الرابط، النطاق، عنوان IP… إلخ.
 *
 * كل الاستدعاءات محاطة بحماية من الأخطاء لأن بعض الحقول تحتاج صلاحيات إضافية
 * (مثل اسم الشبكة على أندرويد 13+).
 */
class WifiNetworkReader(context: Context) {

    private val appContext = context.applicationContext
    private val connectivityManager: ConnectivityManager? =
        appContext.getSystemService(ConnectivityManager::class.java)
    private val wifiManager: WifiManager? =
        appContext.getSystemService(WifiManager::class.java)
    private val telephonyManager: TelephonyManager? =
        appContext.getSystemService(TelephonyManager::class.java)

    fun read(): WifiStatus {
        val manager = connectivityManager ?: return WifiStatus()
        val isWifiToggleOn = runCatching { wifiManager?.isWifiEnabled == true }.getOrDefault(false)
        val isMobileToggleOn = runCatching { telephonyManager?.isDataEnabled == true }.getOrDefault(false)
        val activeNetwork = runCatching { manager.activeNetwork }.getOrNull()
            ?: return WifiStatus(
                isWifiConnected = false,
                isMobileConnected = !isWifiToggleOn && isMobileToggleOn,
            )
        val capabilities = runCatching { manager.getNetworkCapabilities(activeNetwork) }.getOrNull()
        val linkProperties = runCatching { manager.getLinkProperties(activeNetwork) }.getOrNull()

        val isWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val isCellular = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        val isOnline = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        if (!isWifi) {
            return WifiStatus(
                isWifiConnected = false,
                isMobileConnected = isCellular || (!isWifiToggleOn && isMobileToggleOn),
                isOnline = isOnline,
            )
        }

        val wifiInfo = currentWifiInfo(capabilities)
        val rssi = wifiInfo?.rssi?.takeIf { it in -120..0 }
        val signalLevel = rssi?.let { rssiValue ->
            runCatching { WifiManager.calculateSignalLevel(rssiValue, 5) }.getOrDefault(0)
        } ?: 0

        val frequency = wifiInfo?.frequency?.takeIf { it > 0 }
        val ipv4 = linkProperties?.linkAddresses
            ?.firstOrNull { it.address is Inet4Address }
            ?.address
            ?.hostAddress

        return WifiStatus(
            isWifiConnected = true,
            isOnline = isOnline,
            ssid = readSsid(wifiInfo),
            bssid = wifiInfo?.bssid?.takeIf { it.isNotBlank() && it != UNKNOWN_BSSID },
            rssiDbm = rssi,
            signalLevel = signalLevel.coerceIn(0, 4),
            linkSpeedMbps = wifiInfo?.let { info -> runCatching { info.linkSpeed }.getOrNull() }
                ?.takeIf { it > 0 },
            rxLinkSpeedMbps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                wifiInfo?.let { info -> runCatching { info.rxLinkSpeedMbps }.getOrNull() }
                    ?.takeIf { it > 0 }
            } else null,
            txLinkSpeedMbps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                wifiInfo?.let { info -> runCatching { info.txLinkSpeedMbps }.getOrNull() }
                    ?.takeIf { it > 0 }
            } else null,
            frequencyMhz = frequency,
            band = bandOf(frequency),
            standard = readStandard(wifiInfo),
            ipv4 = ipv4,
            gateway = runCatching {
                linkProperties?.routes
                    ?.firstOrNull { it.isDefaultRoute }
                    ?.gateway
                    ?.hostAddress
            }.getOrNull(),
            dnsServers = runCatching {
                linkProperties?.dnsServers?.mapNotNull { it.hostAddress }
            }.getOrNull().orEmpty(),
            downstreamKbps = capabilities?.linkDownstreamBandwidthKbps?.takeIf { it > 0 },
            upstreamKbps = capabilities?.linkUpstreamBandwidthKbps?.takeIf { it > 0 },
        )
    }

    @Suppress("DEPRECATION")
    private fun currentWifiInfo(capabilities: NetworkCapabilities?): WifiInfo? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            capabilities?.transportInfo as? WifiInfo
        } else {
            wifiManager?.connectionInfo
        }
    }.getOrNull()

    private fun readSsid(wifiInfo: WifiInfo?): String? = runCatching {
        val raw = wifiInfo?.ssid ?: return@runCatching null
        if (raw.isEmpty() || raw == UNKNOWN_SSID || raw == "0x") return@runCatching null
        raw.removeSurrounding("\"")
    }.getOrNull()

    private fun readStandard(wifiInfo: WifiInfo?): String? {
        if (wifiInfo == null) return null
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        val standard = runCatching { wifiInfo.wifiStandard }.getOrNull() ?: return null
        return when (standard) {
            ScanResult.WIFI_STANDARD_LEGACY -> "Wi‑Fi 802.11 (قديم)"
            ScanResult.WIFI_STANDARD_11N -> "Wi‑Fi 4 (11n)"
            ScanResult.WIFI_STANDARD_11AC -> "Wi‑Fi 5 (11ac)"
            ScanResult.WIFI_STANDARD_11AX -> "Wi‑Fi 6/6E (11ax)"
            ScanResult.WIFI_STANDARD_11AD -> "WiGig (11ad)"
            ScanResult.WIFI_STANDARD_11BE -> "Wi‑Fi 7 (11be)"
            else -> null
        }
    }

    private fun bandOf(frequencyMhz: Int?): WifiBand {
        val frequency = frequencyMhz ?: return WifiBand.UNKNOWN
        return when (frequency) {
            in 2_400..2_500 -> WifiBand.GHZ_2_4
            in 5_150..5_895 -> WifiBand.GHZ_5
            in 5_925..7_125 -> WifiBand.GHZ_6
            else -> WifiBand.UNKNOWN
        }
    }

    private companion object {
        const val UNKNOWN_SSID = "<unknown ssid>"
        const val UNKNOWN_BSSID = "02:00:00:00:00:00"
    }
}
