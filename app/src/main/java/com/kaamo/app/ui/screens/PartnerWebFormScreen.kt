package com.kaamo.app.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.GeolocationPermissions
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.kaamo.app.data.supabase.SupabaseApi
import com.kaamo.app.ui.theme.KaamoColors

/**
 * The service-provider registration form is the hosted web app (?embed=1), so the admin panel's
 * changes show up here without a new app release. The app hands its sign-in to the page through
 * the [KaamoBridge] and the page tells the app when to close or when the listing was saved.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PartnerWebFormScreen(
    url: String,
    api: SupabaseApi,
    onClose: () -> Unit,
    onDone: (phonePublic: Boolean) -> Unit,
    onOpenExternal: (String) -> Unit,
) {
    val context = LocalContext.current
    val latestClose by rememberUpdatedState(onClose)
    val latestDone by rememberUpdatedState(onDone)
    val latestExternal by rememberUpdatedState(onOpenExternal)
    val host = remember(url) { Uri.parse(url).host.orEmpty() }

    var webView by remember { mutableStateOf<WebView?>(null) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var pendingGeo by remember { mutableStateOf<Pair<String, GeolocationPermissions.Callback>?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        pendingGeo?.let { (origin, callback) -> callback.invoke(origin, grants.values.any { it }, false) }
        pendingGeo = null
    }

    // Back inside the form goes to the previous step; from the first step it leaves the form.
    BackHandler {
        val view = webView
        if (view == null) latestClose()
        else view.evaluateJavascript("(window.kaamoBack && window.kaamoBack()) === true") { handled ->
            if (handled != "true") latestClose()
        }
    }

    DisposableEffect(Unit) { onDispose { webView?.destroy(); webView = null } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KaamoColors.White)
            .statusBarsPadding()
            .imePadding(),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    settings.setGeolocationEnabled(true)
                    setBackgroundColor(android.graphics.Color.WHITE)
                    addJavascriptInterface(
                        KaamoBridge(api, onClose = { latestClose() }, onDone = { latestDone(it) }),
                        "KaamoAndroid",
                    )
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            val target = request.url
                            val inForm = target.scheme == "https" && target.host == host &&
                                (target.path.isNullOrEmpty() || target.path == "/" || target.path == "/index.html")
                            if (inForm) return false
                            // Policy pages and anything else open in the browser, never next to the sign-in bridge.
                            latestExternal(target.toString())
                            return true
                        }

                        override fun onPageStarted(view: WebView, pageUrl: String?, favicon: Bitmap?) {
                            loading = true
                        }

                        override fun onPageFinished(view: WebView, pageUrl: String?) {
                            loading = false
                        }

                        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                            if (request.isForMainFrame) failed = true
                        }
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
                            val granted = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                                .any { ContextCompat.checkSelfPermission(ctx, it) == PackageManager.PERMISSION_GRANTED }
                            if (granted) {
                                callback.invoke(origin, true, false)
                            } else {
                                pendingGeo = origin to callback
                                permissionLauncher.launch(
                                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                                )
                            }
                        }
                    }
                    loadUrl(url)
                    webView = this
                }
            },
        )

        if (loading && !failed) {
            CircularProgressIndicator(color = KaamoColors.Teal, modifier = Modifier.align(Alignment.Center))
        }
        if (failed) {
            Column(
                modifier = Modifier.align(Alignment.Center).padding(32.dp).background(KaamoColors.White),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "फ़ॉर्म नहीं खुल पाया। इंटरनेट जाँचें।\nCouldn't open the form. Check your internet.",
                    textAlign = TextAlign.Center,
                )
                Button(
                    onClick = {
                        failed = false
                        loading = true
                        webView?.loadUrl(url)
                    },
                    modifier = Modifier.padding(top = 16.dp),
                ) { Text("दोबारा कोशिश / Retry") }
                Button(onClick = { latestClose() }, modifier = Modifier.padding(top = 8.dp)) { Text("वापस / Back") }
            }
        }
    }
}

/** What the embedded page may call. Only the app's own https page is ever loaded into this WebView. */
private class KaamoBridge(
    private val api: SupabaseApi,
    private val onClose: () -> Unit,
    private val onDone: (Boolean) -> Unit,
) {
    private val main = Handler(Looper.getMainLooper())

    @JavascriptInterface fun getSession(): String? = api.sessionJson()

    @JavascriptInterface fun saveSession(json: String) {
        runCatching { api.saveSession(json) }
    }

    @JavascriptInterface fun clearSession() = api.clearSession()

    @JavascriptInterface fun close() {
        main.post { onClose() }
    }

    @JavascriptInterface fun done(phonePublic: Boolean) {
        main.post { onDone(phonePublic) }
    }
}
