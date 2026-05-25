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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SignalCellularOff
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madmaxlgndklr.pokedex.ui.theme.PokedexCream
import com.madmaxlgndklr.pokedex.ui.theme.PressStart2P
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class NetworkStatus(val wifi: Boolean, val cellular: Boolean)

@Composable
fun SystemStatusBar(
    isMuted: Boolean,
    onToggleMute: () -> Unit,
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

    Row(
        modifier = modifier.padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onToggleMute, modifier = Modifier.size(24.dp)) {
            Icon(
                imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = if (isMuted) "Unmute" else "Mute",
                tint = PokedexCream,
                modifier = Modifier.size(14.dp)
            )
        }
        Icon(
            imageVector = if (network.wifi) Icons.Filled.Wifi else Icons.Filled.WifiOff,
            contentDescription = "WiFi",
            tint = PokedexCream,
            modifier = Modifier.size(14.dp)
        )
        Icon(
            imageVector = if (network.cellular) Icons.Filled.SignalCellularAlt else Icons.Filled.SignalCellularOff,
            contentDescription = "Cellular",
            tint = PokedexCream,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = "$batteryPct%",
            fontFamily = PressStart2P,
            fontSize = 6.sp,
            color = PokedexCream
        )
        Text(
            text = currentTime,
            fontFamily = PressStart2P,
            fontSize = 6.sp,
            color = PokedexCream
        )
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
