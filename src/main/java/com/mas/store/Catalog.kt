package com.mas.store

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class L(val en: String, val ar: String) {
    fun get(isAr: Boolean): String = if (isAr && ar.isNotBlank()) ar else en
}

data class AppItem(
    val id: String,
    val pkg: String,
    val name: L,
    val subtitle: L,
    val category: String,
    val icon: String,
    val color: Long,
    val screenshots: List<String>,
    val apk: String,
    val version: String,
    val versionCode: Long,
    val size: Long,
    val sha256: String,
    val description: L,
    val whatsNew: L,
    val featured: Boolean,
)

private fun JSONObject.l(key: String): L {
    val o = optJSONObject(key) ?: return L("", "")
    return L(o.optString("en"), o.optString("ar"))
}

private fun parseColor(s: String): Long = try {
    java.lang.Long.parseLong(s.trim().removePrefix("#"), 16) or 0xFF000000L
} catch (e: Exception) {
    0xFF4F8BFFL
}

fun parseCatalog(json: String): List<AppItem> {
    val arr = JSONObject(json).optJSONArray("apps") ?: return emptyList()
    val out = ArrayList<AppItem>()
    for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        val shots = ArrayList<String>()
        val sa = o.optJSONArray("screenshots")
        if (sa != null) for (j in 0 until sa.length()) shots.add(sa.getString(j))
        out.add(
            AppItem(
                id = o.optString("id"),
                pkg = o.optString("package"),
                name = o.l("name"),
                subtitle = o.l("subtitle"),
                category = o.optString("category", "app"),
                icon = o.optString("icon"),
                color = parseColor(o.optString("color", "#4F8BFF")),
                screenshots = shots,
                apk = o.optString("apk"),
                version = o.optString("version"),
                versionCode = o.optLong("versionCode", 0),
                size = o.optLong("size", 0),
                sha256 = o.optString("sha256"),
                description = o.l("description"),
                whatsNew = o.l("whatsNew"),
                featured = o.optBoolean("featured", false),
            )
        )
    }
    return out
}

object CatalogRepo {
    const val REMOTE = "https://raw.githubusercontent.com/mas2026zoom-dotcom/mas-store/main/catalog/apps.json"

    fun bundled(ctx: Context): List<AppItem> = try {
        parseCatalog(ctx.assets.open("apps.json").bufferedReader().use { it.readText() })
    } catch (e: Exception) {
        emptyList()
    }

    fun cached(ctx: Context): List<AppItem>? = try {
        val f = File(ctx.filesDir, "apps.json")
        if (f.exists()) parseCatalog(f.readText()) else null
    } catch (e: Exception) {
        null
    }

    /** Returns null when offline or the file is invalid. */
    fun fetchRemote(ctx: Context): List<AppItem>? = try {
        val c = URL(REMOTE + "?t=" + System.currentTimeMillis()).openConnection() as HttpURLConnection
        c.connectTimeout = 8000
        c.readTimeout = 8000
        val text = c.inputStream.bufferedReader().use { it.readText() }
        val list = parseCatalog(text)
        File(ctx.filesDir, "apps.json").writeText(text)
        list
    } catch (e: Exception) {
        null
    }
}
