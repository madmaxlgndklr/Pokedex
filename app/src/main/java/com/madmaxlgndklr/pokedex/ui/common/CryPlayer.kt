package com.madmaxlgndklr.pokedex.ui.common

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.madmaxlgndklr.pokedex.data.remote.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

object CryPlayer {
    private const val CDN = "https://play.pokemonshowdown.com/audio/cries"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var appContext: Context
    private lateinit var networkObserver: NetworkObserver
    private var player: ExoPlayer? = null

    fun init(context: Context, observer: NetworkObserver) {
        appContext = context.applicationContext
        networkObserver = observer
    }

    private fun criesDir(): File =
        File(appContext.filesDir, "cries").also { it.mkdirs() }

    private fun cryFile(name: String) = File(criesDir(), "$name.ogg")

    suspend fun isCryAvailable(name: String): Boolean {
        if (!::networkObserver.isInitialized) return false
        return withContext(Dispatchers.IO) { cryFile(name).exists() } || networkObserver.isOnline.value
    }

    fun play(name: String) {
        if (!::appContext.isInitialized) return
        val file = cryFile(name)
        val uri = if (file.exists()) Uri.fromFile(file)
                  else Uri.parse("$CDN/$name.ogg")
        Handler(Looper.getMainLooper()).post {
            if (player == null) {
                player = ExoPlayer.Builder(appContext).build()
            }
            player!!.apply {
                stop()
                clearMediaItems()
                setMediaItem(MediaItem.fromUri(uri))
                prepare()
                play()
            }
        }
        // Cache streamed audio in background so next play is local
        if (!file.exists()) {
            scope.launch { downloadCry(name) }
        }
    }

    fun stop() {
        Handler(Looper.getMainLooper()).post { player?.stop() }
    }

    fun release() {
        Handler(Looper.getMainLooper()).post {
            player?.release()
            player = null
        }
    }

    suspend fun downloadCry(name: String): Boolean {
        if (!::appContext.isInitialized) return false
        val file = cryFile(name)
        if (file.exists() && file.length() > 0) return true
        return try {
            val request = Request.Builder().url("$CDN/$name.ogg").build()
            val response = withContext(Dispatchers.IO) {
                RetrofitClient.httpClient.newCall(request).execute()
            }
            response.use { resp ->
                if (!resp.isSuccessful) return@use false
                val bytes = resp.body?.bytes() ?: return@use false
                if (bytes.isEmpty()) return@use false
                val tmp = File(criesDir(), "$name.ogg.tmp")
                tmp.writeBytes(bytes)
                tmp.renameTo(file)
                true
            }
        } catch (_: Exception) { false }
    }

    suspend fun syncCries(names: List<String>, onProgress: (Int, Int) -> Unit) {
        if (!::appContext.isInitialized) return
        val total = names.size
        val completed = AtomicInteger(0)
        val semaphore = Semaphore(40)
        kotlinx.coroutines.coroutineScope {
            names.forEach { name ->
                launch(Dispatchers.IO) {
                    semaphore.withPermit {
                        downloadCry(name)
                        val n = completed.incrementAndGet()
                        onProgress(n, total)
                    }
                }
            }
        }
    }
}
