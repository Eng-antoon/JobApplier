package com.aplicator.jobapplier.ui.webextract

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.aplicator.jobapplier.ui.theme.JobApplierTheme
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class ExtractedData(
    val title: String? = null,
    val company: String? = null,
    val description: String? = null,
    val success: Boolean = false,
)

class WebJobExtractorActivity : ComponentActivity() {

    private var webView: WebView? = null

    companion object {
        const val EXTRA_URL = "extra_url"
        const val RESULT_TITLE = "result_title"
        const val RESULT_COMPANY = "result_company"
        const val RESULT_DESCRIPTION = "result_description"

        private const val EXTRACTION_JS = """
            (function() {
                function getText(selectors) {
                    for (var i = 0; i < selectors.length; i++) {
                        var el = document.querySelector(selectors[i]);
                        if (el) {
                            var text = el.innerText || el.textContent;
                            if (text && text.trim().length > 20) return text.trim();
                        }
                    }
                    return null;
                }
                var title = getText([
                    '.job-details-jobs-unified-top-card__job-title',
                    '.top-card-layout__title',
                    '.jobs-unified-top-card__job-title',
                    'h1.t-24', 'h1'
                ]);
                var company = getText([
                    '.job-details-jobs-unified-top-card__company-name',
                    '.topcard__org-name-link',
                    '.jobs-unified-top-card__company-name',
                    '.topcard__flavor--black-link',
                    'a[data-tracking-control-name="public_jobs_topcard-org-name"]'
                ]);
                var description = getText([
                    '.jobs-description-content__text',
                    '.show-more-less-html__markup',
                    '.description__text',
                    '.jobs-box__html-content',
                    '#job-details',
                    'article'
                ]);
                ExtractorBridge.onDataExtracted(JSON.stringify({
                    title: title,
                    company: company,
                    description: description,
                    success: !!(title || description)
                }));
            })();
        """
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val url = intent.getStringExtra(EXTRA_URL)
        if (url.isNullOrBlank()) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)

        setContent {
            JobApplierTheme {
                var progress by remember { mutableFloatStateOf(0f) }
                var pageLoaded by remember { mutableStateOf(false) }
                var showFab by remember { mutableStateOf(false) }
                var isExtracting by remember { mutableStateOf(false) }
                var currentUrl by remember { mutableStateOf(url) }
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Extract Job", style = MaterialTheme.typography.titleMedium) },
                            navigationIcon = {
                                IconButton(onClick = {
                                    if (webView?.canGoBack() == true) {
                                        webView?.goBack()
                                    } else {
                                        setResult(RESULT_CANCELED)
                                        finish()
                                    }
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            },
                        )
                    },
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    floatingActionButton = {
                        if (showFab && !isExtracting) {
                            ExtendedFloatingActionButton(
                                onClick = {
                                    isExtracting = true
                                    webView?.evaluateJavascript(EXTRACTION_JS, null)
                                },
                                icon = { Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                text = { Text("Extract Job") },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    },
                ) { paddingValues ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                    ) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { context ->
                                WebView(context).apply {
                                    webView = this

                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.loadWithOverviewMode = true
                                    settings.useWideViewPort = true
                                    settings.setSupportZoom(true)
                                    settings.builtInZoomControls = true
                                    settings.displayZoomControls = false
                                    settings.userAgentString = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

                                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                                    addJavascriptInterface(
                                        ExtractorBridge { data ->
                                            runOnUiThread { handleExtractedData(data) }
                                        },
                                        "ExtractorBridge",
                                    )

                                    webViewClient = object : WebViewClient() {
                                        override fun onPageStarted(view: WebView?, pageUrl: String?, favicon: Bitmap?) {
                                            pageLoaded = false
                                            showFab = false
                                        }

                                        override fun onPageFinished(view: WebView?, pageUrl: String?) {
                                            pageLoaded = true
                                            currentUrl = pageUrl ?: currentUrl
                                            val isAuthPage = pageUrl?.contains("/login") == true ||
                                                pageUrl?.contains("/authwall") == true ||
                                                pageUrl?.contains("/checkpoint") == true
                                            if (!isAuthPage) {
                                                view?.postDelayed({ showFab = true }, 2500)
                                            }
                                        }

                                        override fun onReceivedError(
                                            view: WebView?,
                                            request: WebResourceRequest?,
                                            error: WebResourceError?,
                                        ) {
                                            if (request?.isForMainFrame == true) {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("Page failed to load. Check your connection.")
                                                }
                                            }
                                        }
                                    }

                                    webChromeClient = object : WebChromeClient() {
                                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                            progress = newProgress / 100f
                                        }
                                    }

                                    loadUrl(url)
                                }
                            },
                        )

                        if (!pageLoaded) {
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.TopCenter),
                            )
                        }
                    }
                }
            }
        }
    }

    private fun handleExtractedData(json: String) {
        try {
            val parser = Json { ignoreUnknownKeys = true }
            val data = parser.decodeFromString<ExtractedData>(json)
            if (data.success && (data.title != null || data.description != null)) {
                val resultIntent = Intent().apply {
                    putExtra(RESULT_TITLE, data.title)
                    putExtra(RESULT_COMPANY, data.company)
                    putExtra(RESULT_DESCRIPTION, data.description)
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            } else {
                webView?.evaluateJavascript(
                    "(function() { ExtractorBridge.onFallbackExtracted(document.body.innerText || ''); })();",
                    null,
                )
            }
        } catch (_: Exception) {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    private fun handleFallbackText(text: String) {
        if (text.length > 100) {
            val resultIntent = Intent().apply {
                putExtra(RESULT_DESCRIPTION, text.take(15000))
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        } else {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        CookieManager.getInstance().flush()
    }

    @Deprecated("Use OnBackPressedCallback")
    override fun onBackPressed() {
        if (webView?.canGoBack() == true) {
            webView?.goBack()
        } else {
            setResult(RESULT_CANCELED)
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        webView?.destroy()
        webView = null
        super.onDestroy()
    }

    private class ExtractorBridge(private val onExtracted: (String) -> Unit) {
        @JavascriptInterface
        fun onDataExtracted(json: String) {
            onExtracted(json)
        }

        @JavascriptInterface
        fun onFallbackExtracted(text: String) {
            onExtracted("""{"title":null,"company":null,"description":${Json.encodeToString(kotlinx.serialization.serializer<String>(), text)},"success":true}""")
        }
    }
}
