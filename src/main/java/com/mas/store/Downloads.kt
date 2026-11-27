package com.mas.store

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.mutableStateMapOf
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

sealed interface Dl {
    data class Prog(val done: Long, val total: Long) : Dl
    data class Ready(val path: String) : Dl
    data class Err(val msg: String) : Dl
}

object Downloads {
    val states = mutableStateMapOf<String, Dl>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun installedVersion(ctx: Context, pkg: String): Long? = try {
        ctx.packageManager.getPackageInfo(pkg, 0).longVersionCode
    } catch (e: Exception) {
        null
    }

    fun open(ctx: Context, pkg: String) {
        val i = ctx.packageManager.getLaunchIntentForPackage(pkg) ?: return
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(i)
    }

    fun install(ctx: Context, path: String) {
        val file = File(path)
        if (!file.exists()) return
        if (!ctx.packageManager.canRequestPackageInstalls()) {
            val s = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + ctx.packageName))
            s.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(s)
            return
        }
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", file)
        val i = Intent(Intent.ACTION_VIEW)
        i.setDataAndType(uri, "application/vnd.android.package-archive")
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(i)
    }

    fun start(ctx: Context, app: AppItem) {
        if (states[app.id] is Dl.Prog) return
        val appCtx = ctx.applicationContext
        states[app.id] = Dl.Prog(0, app.size)
        scope.launch {
            try {
                val dir = File(appCtx.cacheDir, "apks")
                dir.mkdirs()
                val part = File(dir, app.id + ".apk.part")
                val out = File(dir, app.id + ".apk")
                val c = URL(app.apk).openConnection() as HttpURLConnection
                c.connectTimeout = 20000
                c.readTimeout = 30000
                c.instanceFollowRedirects = true
                val code = c.responseCode
                if (code !in 200..299) throw IllegalStateException("HTTP $code")
                val total = if (c.contentLengthLong > 0) c.contentLengthLong else app.size
                val md = MessageDigest.getInstance("SHA-256")
                var done = 0L
                c.inputStream.use { input ->
                    part.outputStream().use { o ->
                        val buf = ByteArray(64 * 1024)
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            o.write(buf, 0, n)
                            md.update(buf, 0, n)
                            done += n
                            states[app.id] = Dl.Prog(done, total)
                        }
                    }
                }
                if (app.sha256.isNotBlank()) {
                    val hex = md.digest().joinToString("") { "%02x".format(it) }
                    if (!hex.equals(app.sha256.trim(), ignoreCase = true)) {
                        part.delete()
                        throw IllegalStateException("sha256 mismatch")
                    }
                }
                if (out.exists()) out.delete()
                part.renameTo(out)
                states[app.id] = Dl.Ready(out.absolutePath)
                install(appCtx, out.absolutePath)
            } catch (e: Exception) {
                states[app.id] = Dl.Err(e.javaClass.simpleName + ": " + (e.message ?: ""))
            }
        }
    }
}
