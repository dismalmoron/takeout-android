package com.takeoutindex.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persists the list of saved servers and which one is active, in SharedPreferences.
 *
 * A server is just a base URL plus a display label. The address is entered once;
 * additional servers can be added and switched between, satisfying the
 * "enter once, change later, save alternates" requirement. Credentials are NOT
 * stored here — login is handled by the server's own /login page and the session
 * cookie is persisted separately (see MainActivity's CookieManager use), so the
 * app stays logged in until the user logs out.
 */
data class Server(val label: String, val url: String)

class ServerStore(context: Context) {

    private val prefs = context.getSharedPreferences("servers", Context.MODE_PRIVATE)

    fun list(): List<Server> {
        val raw = prefs.getString(KEY_LIST, null) ?: return emptyList()
        val arr = JSONArray(raw)
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Server(o.getString("label"), o.getString("url"))
        }
    }

    fun activeUrl(): String? = prefs.getString(KEY_ACTIVE, null)

    fun setActive(url: String) {
        prefs.edit().putString(KEY_ACTIVE, url).apply()
    }

    /** Add or update a server by URL, then make it active. Returns normalised URL. */
    fun saveAndActivate(label: String, rawUrl: String): String {
        val url = normalise(rawUrl)
        val current = list().toMutableList()
        val idx = current.indexOfFirst { it.url == url }
        val entry = Server(label.ifBlank { hostOf(url) }, url)
        if (idx >= 0) current[idx] = entry else current.add(entry)
        persist(current)
        setActive(url)
        return url
    }

    fun remove(url: String) {
        val current = list().filterNot { it.url == url }
        persist(current)
        if (activeUrl() == url) {
            prefs.edit().putString(KEY_ACTIVE, current.firstOrNull()?.url).apply()
        }
    }

    private fun persist(servers: List<Server>) {
        val arr = JSONArray()
        servers.forEach {
            arr.put(JSONObject().put("label", it.label).put("url", it.url))
        }
        prefs.edit().putString(KEY_LIST, arr.toString()).apply()
    }

    companion object {
        private const val KEY_LIST = "list"
        private const val KEY_ACTIVE = "active"

        /** Add a scheme if missing, strip a trailing slash. */
        fun normalise(raw: String): String {
            var u = raw.trim()
            if (!u.startsWith("http://") && !u.startsWith("https://")) {
                u = "http://$u"
            }
            return u.trimEnd('/')
        }

        fun hostOf(url: String): String =
            url.substringAfter("://").substringBefore('/')
    }
}
