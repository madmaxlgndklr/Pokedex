package com.madmaxlgndklr.pokedex.ui.common

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.BatteryManager
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SignalCellularOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madmaxlgndklr.pokedex.ui.theme.PressStart2P
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

private data class NetworkStatus(val wifi: Boolean, val cellular: Boolean)

private val NeonGreen = Color(0xFF39FF14)

@Composable
fun SystemStatusBar(
    isMuted: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val currentTime by produceState(initialValue = formattedTime()) {
        while (true) {
            delay(30_000)
            value = formattedTime()
        }
    }

    val batteryPct by produceState(initialValue = readBattery(context)) {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level >= 0 && scale > 0) value = level * 100 / scale
            }
        }
        context.registerReceiver(receiver, filter)
        awaitDispose { context.unregisterReceiver(receiver) }
    }

    val network by produceState(initialValue = readNetwork(context)) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(n: Network) { value = readNetwork(context) }
            override fun onLost(n: Network) { value = readNetwork(context) }
            override fun onCapabilitiesChanged(n: Network, caps: NetworkCapabilities) {
                value = NetworkStatus(
                    wifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI),
                    cellular = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                )
            }
        }
        cm.registerNetworkCallback(NetworkRequest.Builder().build(), callback)
        awaitDispose { cm.unregisterNetworkCallback(callback) }
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val swF = maxWidth.value
        val shF = maxHeight.value

        val arcCx = swF / 2f
        val arcCy = shF * 0.20f
        val arcR  = swF * 0.43f

        fun arcPos(angleDeg: Float): Pair<Float, Float> {
            val rad = Math.toRadians(angleDeg.toDouble())
            return (arcCx + arcR * sin(rad).toFloat()) to (arcCy - arcR * cos(rad).toFloat())
        }

        fun launch(action: String) = context.startActivity(Intent(action))
        fun noRipple() = MutableInteractionSource()

        // Mute — sound settings
        val (mx, my) = arcPos(-62f)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset((mx - 12f).dp, (my - 12f).dp)
                .size(24.dp)
                .clickable(interactionSource = remember { noRipple() }, indication = null) {
                    launch(Settings.ACTION_SOUND_SETTINGS)
                }
        ) {
            Icon(
                imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff
                              else Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = "Sound settings",
                tint = NeonGreen,
                modifier = Modifier.size(16.dp)
            )
        }

        // WiFi — wifi settings
        val (wx, wy) = arcPos(-31f)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset((wx - 12f).dp, (wy - 12f).dp)
                .size(24.dp)
                .clickable(interactionSource = remember { noRipple() }, indication = null) {
                    launch(Settings.ACTION_WIFI_SETTINGS)
                }
        ) {
            Icon(
                imageVector = if (network.wifi) Icons.Filled.Wifi else Icons.Filled.WifiOff,
                contentDescription = "WiFi settings",
                tint = NeonGreen,
                modifier = Modifier.size(16.dp)
            )
        }

        // Cellular — wireless settings
        val (cellX, cellY) = arcPos(0f)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset((cellX - 12f).dp, (cellY - 12f).dp)
                .size(24.dp)
                .clickable(interactionSource = remember { noRipple() }, indication = null) {
                    launch(Settings.ACTION_WIRELESS_SETTINGS)
                }
        ) {
            Icon(
                imageVector = if (network.cellular) Icons.Filled.SignalCellularAlt
                              else Icons.Filled.SignalCellularOff,
                contentDescription = "Network settings",
                tint = NeonGreen,
                modifier = Modifier.size(16.dp)
            )
        }

        // Battery — battery usage
        val (battX, battY) = arcPos(31f)
        Box(
            modifier = Modifier
                .offset((battX - 16f).dp, (battY - 7f).dp)
                .clickable(interactionSource = remember { noRipple() }, indication = null) {
                    launch(Intent.ACTION_POWER_USAGE_SUMMARY)
                }
        ) {
            Text(
                text = "$batteryPct%",
                fontFamily = PressStart2P,
                fontSize = 7.sp,
                color = NeonGreen
            )
        }

        // Time — date & time settings
        val (timeX, timeY) = arcPos(62f)
        Box(
            modifier = Modifier
                .offset((timeX - 24f).dp, (timeY - 7f).dp)
                .clickable(interactionSource = remember { noRipple() }, indication = null) {
                    launch(Settings.ACTION_DATE_SETTINGS)
                }
        ) {
            Text(
                text = currentTime,
                fontFamily = PressStart2P,
                fontSize = 7.sp,
                color = NeonGreen
            )
        }
    }
}

private fun formattedTime(): String =
    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())

private fun readBattery(context: Context): Int {
    val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    return if (level >= 0 && scale > 0) level * 100 / scale else 0
}

private fun readNetwork(context: Context): NetworkStatus {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val caps = cm.getNetworkCapabilities(cm.activeNetwork)
    return NetworkStatus(
        wifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true,
        cellular = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
    )
}
