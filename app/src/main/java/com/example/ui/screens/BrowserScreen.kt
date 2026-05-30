package com.example.ui.screens

import android.app.DownloadManager
import android.view.ViewGroup
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.example.ui.PortalViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    viewModel: PortalViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUrl by viewModel.currentUrl.collectAsState()
    
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var pageTitle by remember { mutableStateOf("Loading...") }
    var pageUrl by remember { mutableStateOf(currentUrl) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var isLoadingPage by remember { mutableStateOf(false) }
    var pageProgress by remember { mutableStateOf(0) }

    // Dialog state for configuring offline study downloads
    var showDownloadDialog by remember { mutableStateOf(false) }
    var dlSubject by remember { mutableStateOf("") }
    var dlYear by remember { mutableStateOf("2024") }
    var dlLevel by remember { mutableStateOf("O-Level (Form 4)") }

    // Observe bookmark status reactive to pageUrl changes
    val isCurrentBookmarked by viewModel.isBookmarkedFlow(pageUrl).collectAsState(initial = false)

    // Automatically guess subject name from the webpage title for premium UX prefilling
    LaunchedEffect(pageTitle, showDownloadDialog) {
        if (showDownloadDialog && dlSubject.isBlank()) {
            val guessed = when {
                pageTitle.contains("Physics", ignoreCase = true) -> "Physics"
                pageTitle.contains("Chemistry", ignoreCase = true) -> "Chemistry"
                pageTitle.contains("Biology", ignoreCase = true) -> "Biology"
                pageTitle.contains("Mathematics", ignoreCase = true) -> "Mathematics"
                pageTitle.contains("Geography", ignoreCase = true) -> "Geography"
                pageTitle.contains("History", ignoreCase = true) -> "History"
                pageTitle.contains("English", ignoreCase = true) -> "English Language"
                pageTitle.contains("Kiswahili", ignoreCase = true) -> "Kiswahili"
                pageTitle.contains("Civics", ignoreCase = true) -> "Civics"
                else -> ""
            }
            dlSubject = guessed
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = pageTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = pageUrl,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            webViewRef?.loadUrl("https://elimupdf.co.tz")
                        }) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "ElimuPDF Home",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    actions = {
                        // Dynamic save bookmark control
                        IconButton(
                            onClick = {
                                viewModel.toggleBookmark(
                                    title = if (pageTitle.isBlank() || pageTitle == "Loading...") "ElimuPDF Resource" else pageTitle,
                                    url = pageUrl,
                                    category = "PORTAL_VISIT"
                                )
                            },
                            modifier = Modifier.testTag("browser_bookmark_button")
                        ) {
                            Icon(
                                imageVector = if (isCurrentBookmarked) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = if (isCurrentBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // Universal Web URL Share button
                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, pageTitle)
                                putExtra(Intent.EXTRA_TEXT, pageUrl)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Link"))
                        }) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "Share URL")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
                
                // Fine linear progression overlay
                AnimatedVisibility(
                    visible = isLoadingPage,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    LinearProgressIndicator(
                        progress = { pageProgress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        },
        bottomBar = {
            Surface(
                tonalElevation = 4.dp,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { webViewRef?.goBack() },
                        enabled = canGoBack,
                        modifier = Modifier.testTag("browser_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBackIosNew,
                            contentDescription = "Go Back",
                            tint = if (canGoBack) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        )
                    }

                    IconButton(
                        onClick = { webViewRef?.goForward() },
                        enabled = canGoForward,
                        modifier = Modifier.testTag("browser_forward_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForwardIos,
                            contentDescription = "Go Forward",
                            tint = if (canGoForward) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        )
                    }

                    IconButton(
                        onClick = {
                            if (isLoadingPage) {
                                webViewRef?.stopLoading()
                            } else {
                                webViewRef?.reload()
                            }
                        },
                        modifier = Modifier.testTag("browser_reload_button")
                    ) {
                        Icon(
                            imageVector = if (isLoadingPage) Icons.Default.Close else Icons.Default.Refresh,
                            contentDescription = "Refresh or Stop",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { webViewRef?.loadUrl("https://elimupdf.co.tz") },
                        modifier = Modifier.testTag("browser_home_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "ElimuPDF Official Website",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Open directly in system/external browser as backup safety trigger
                    Button(
                        onClick = {
                            try {
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(pageUrl))
                                context.startActivity(browserIntent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error opening system browser", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                            contentColor = MaterialTheme.colorScheme.secondary
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp).testTag("open_external_browser_button")
                    ) {
                        Text("External", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // High efficiency Android WebView rendering block
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            builtInZoomControls = true
                            displayZoomControls = false
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            databaseEnabled = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                        }

                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isLoadingPage = true
                                url?.let { pageUrl = it }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoadingPage = false
                                url?.let { pageUrl = it }
                                title?.let { pageTitle = it }
                                canGoBack = view?.canGoBack() ?: false
                                canGoForward = view?.canGoForward() ?: false
                            }

                            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                if (url != null) {
                                    if (url.endsWith(".pdf", ignoreCase = true) || url.contains("/download")) {
                                        triggerPdfDownload(ctx, url)
                                        return true
                                    }
                                    view?.loadUrl(url)
                                }
                                return true
                            }

                            @Deprecated("Deprecated in Java")
                            override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                                val url = request?.url?.toString()
                                if (url != null) {
                                    if (url.endsWith(".pdf", ignoreCase = true) || url.contains("/download")) {
                                        triggerPdfDownload(ctx, url)
                                        return true
                                    }
                                    view?.loadUrl(url)
                                }
                                return true
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                super.onProgressChanged(view, newProgress)
                                pageProgress = newProgress
                            }

                            override fun onReceivedTitle(view: WebView?, webTitle: String?) {
                                super.onReceivedTitle(view, webTitle)
                                webTitle?.let { pageTitle = it }
                            }
                        }

                        setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                            triggerPdfDownload(ctx, url, contentDisposition, mimetype)
                        }

                        loadUrl(currentUrl)
                        webViewRef = this
                    }
                },
                update = { webView ->
                    if (webView.url != currentUrl) {
                        webView.loadUrl(currentUrl)
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("in_app_webview")
            )

            // Dynamic Offline-Save Floating Action Button
            FloatingActionButton(
                onClick = { 
                    dlSubject = "" // trigger auto-guesser re-run
                    showDownloadDialog = true 
                },
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .padding(bottom = 60.dp) // shift up slightly above navigation bar
                    .size(56.dp)
                    .testTag("offline_download_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.DownloadForOffline,
                    contentDescription = "Save PDF Offline for Study"
                )
            }
        }
    }

    // Material Design 3 study file configuration dialog
    if (showDownloadDialog) {
        Dialog(onDismissRequest = { showDownloadDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = "Config Download",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Save Offline Past Paper",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Customize the descriptive catalog parameters for saving this study paper for completely connectivity-free reading.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = dlSubject.ifBlank { "" },
                        onValueChange = { dlSubject = it },
                        label = { Text("Subject (e.g. Physics, History)") },
                        placeholder = { Text("Enter field subject") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = dlYear,
                        onValueChange = { dlYear = it },
                        label = { Text("Year of Exam") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = dlLevel,
                        onValueChange = { dlLevel = it },
                        label = { Text("Academic Level (O-Level, A-Level...)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showDownloadDialog = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val finalSubject = dlSubject.ifBlank { "Educational Resource" }
                                val finalTitle = "NECTA $dlLevel $finalSubject ($dlYear)"
                                
                                viewModel.downloadMockPaper(
                                    context = context,
                                    title = finalTitle,
                                    subject = finalSubject,
                                    year = dlYear,
                                    level = dlLevel,
                                    fileUrl = pageUrl
                                )
                                showDownloadDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("Download Offline")
                        }
                    }
                }
            }
        }
    }
}

private fun triggerPdfDownload(
    context: Context,
    url: String,
    contentDisposition: String? = null,
    mimetype: String? = null
) {
    try {
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            val fileName = URLUtil.guessFileName(url, contentDisposition, mimetype ?: "application/pdf")
            setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
            setTitle("Downloading $fileName")
            setDescription("ElimuPDF Resource Archive File")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            
            val cookies = CookieManager.getInstance().getCookie(url)
            addRequestHeader("cookie", cookies)
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
        Toast.makeText(context, "Downloading PDF started. Check notifications...", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Download failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (inner: Exception) {
            Toast.makeText(context, "Could not resolve download url.", Toast.LENGTH_SHORT).show()
        }
    }
}
