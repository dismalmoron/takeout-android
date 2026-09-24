package com.takeoutindex.app

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebResourceError
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.takeoutindex.app.databinding.ActivityMainBinding

/**
 * The whole client is the server's own web UI loaded in a hardened WebView.
 * Nothing about search/labels/reader is reimplemented here; this class handles
 * only what a browser tab cannot: choosing a server, persisting the login
 * session across launches, caching, and native back navigation.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var store: ServerStore
    private var currentUrl: String? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        store = ServerStore(this)

        // No server configured yet -> go set one up first.
        val active = store.activeUrl()
        if (active == null) {
            openServerPicker()
            return
        }
        currentUrl = active
        setSupportActionBar(binding.toolbar)
        configureWebView()
        wireRefresh()
        wireBack()
        binding.webview.loadUrl(active)
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_servers -> {
                startActivity(Intent(this, ServerActivity::class.java)); true
            }
            R.id.action_clear_cache -> {
                binding.webview.clearCache(true)
                Toast.makeText(this, "Cache cleared", Toast.LENGTH_SHORT).show()
                binding.webview.reload(); true
            }
            R.id.action_reload -> { binding.webview.reload(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun configureWebView() {
        val wv = binding.webview
        wv.settings.apply {
            javaScriptEnabled = true          // the UI is a JS single-page app
            domStorageEnabled = true          // localStorage used by the UI
            // Thumbnail caching: use the HTTP cache, falling back to it when
            // offline. Combined with the server's long-lived cache headers on
            // /api/photo/{id}/thumb, thumbnails load from disk on repeat views.
            cacheMode = WebSettings.LOAD_DEFAULT
            databaseEnabled = true
            loadsImagesAutomatically = true
            mediaPlaybackRequiresUserGesture = false
            useWideViewPort = true
            loadWithOverviewMode = true
            // Zoom available but no on-screen controls cluttering the UI.
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
        }

        // Persist cookies (the session) across app restarts, so the user stays
        // logged in until they use the app's own Logout. This is the mechanism
        // behind the "stay logged in unless manually logged out" requirement.
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(wv, true)
        }

        wv.webViewClient = object : WebViewClient() {
            // Keep same-origin navigation inside the WebView; the app is a
            // single server, so this is nearly everything.
            override fun shouldOverrideUrlLoading(
                view: WebView, request: WebResourceRequest
            ): Boolean {
                val target = request.url.toString()
                val base = currentUrl ?: return false
                return if (sameHost(target, base)) {
                    false                      // load in-app
                } else {
                    // External link -> hand to the system browser.
                    startActivity(Intent(Intent.ACTION_VIEW, request.url))
                    true
                }
            }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                binding.refresh.isRefreshing = false
                binding.progress.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView, url: String) {
                binding.progress.visibility = View.GONE
                CookieManager.getInstance().flush()   // persist session to disk
            }

            override fun onReceivedError(
                view: WebView, request: WebResourceRequest, error: WebResourceError
            ) {
                if (request.isForMainFrame) {
                    binding.progress.visibility = View.GONE
                    Toast.makeText(
                        this@MainActivity,
                        "Cannot reach server. Check the address in Settings.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            // A self-signed HTTPS cert only proceeds if the user installed it as
            // a user CA (trusted via network_security_config). An untrusted cert
            // is rejected rather than blindly accepted.
            override fun onReceivedSslError(
                view: WebView, handler: SslErrorHandler, error: SslError
            ) {
                handler.cancel()
                Toast.makeText(
                    this@MainActivity,
                    "TLS certificate not trusted. Install the server's certificate, " +
                        "or use HTTP on the LAN.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun wireRefresh() {
        binding.refresh.setOnRefreshListener { binding.webview.reload() }
    }

    private fun wireBack() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.webview.canGoBack()) binding.webview.goBack()
                else finish()
            }
        })
    }

    private fun openServerPicker() {
        startActivity(Intent(this, ServerActivity::class.java))
        finish()
    }

    override fun onResume() {
        super.onResume()
        // If the active server changed in Settings, reload the new one.
        val active = store.activeUrl()
        if (active != null && active != currentUrl) {
            currentUrl = active
            binding.webview.loadUrl(active)
        }
    }

    private fun sameHost(a: String, b: String): Boolean =
        ServerStore.hostOf(ServerStore.normalise(a)) ==
            ServerStore.hostOf(ServerStore.normalise(b))
}
