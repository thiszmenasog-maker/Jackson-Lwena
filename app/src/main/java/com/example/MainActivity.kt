package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.AppDatabase
import com.example.data.BookmarkRepository
import com.example.ui.PortalViewModel
import com.example.ui.PortalViewModelFactory
import com.example.ui.screens.BookmarksScreen
import com.example.ui.screens.BrowserScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SmsGuideScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.collectLatest

enum class NavigationTab {
    HOME, BROWSER, BOOKMARKS, OFFLINE_GUIDE
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // Initialize database and repository inside composition safely
                val context = LocalContext.current
                val database = remember { AppDatabase.getDatabase(context) }
                val repository = remember {
                    BookmarkRepository(
                        database.bookmarkDao(),
                        database.downloadedPaperDao(),
                        database.userDao(),
                        database.appNotificationDao()
                    )
                }
                val viewModel: PortalViewModel = viewModel(
                    factory = PortalViewModelFactory(repository)
                )

                // Observe Toast messages from inside composition/ViewModel flow
                LaunchedEffect(viewModel.toastMessage) {
                    viewModel.toastMessage.collectLatest { message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }

                // Unified navigation layout
                var activeTab by remember { mutableStateOf(NavigationTab.HOME) }

                // Intercept deep links from tapped heads-up push announcements
                LaunchedEffect(intent) {
                    val targetUrl = intent?.getStringExtra("LAUNCH_URL")
                    if (!targetUrl.isNullOrBlank()) {
                        viewModel.loadUrl(targetUrl)
                        activeTab = NavigationTab.BROWSER
                    }
                }

                Scaffold(
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("bottom_navigation_bar")
                        ) {
                            NavigationBarItem(
                                selected = activeTab == NavigationTab.HOME,
                                onClick = { activeTab = NavigationTab.HOME },
                                icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "Home") },
                                label = { Text("Home") },
                                modifier = Modifier.testTag("nav_home_tab")
                            )
                            NavigationBarItem(
                                selected = activeTab == NavigationTab.BROWSER,
                                onClick = { activeTab = NavigationTab.BROWSER },
                                icon = { Icon(imageVector = Icons.Default.Public, contentDescription = "Browser") },
                                label = { Text("Browser") },
                                modifier = Modifier.testTag("nav_browser_tab")
                            )
                            NavigationBarItem(
                                selected = activeTab == NavigationTab.BOOKMARKS,
                                onClick = { activeTab = NavigationTab.BOOKMARKS },
                                icon = { Icon(imageVector = Icons.Default.Bookmark, contentDescription = "Bookmarks") },
                                label = { Text("Saved") },
                                modifier = Modifier.testTag("nav_saved_tab")
                            )
                            NavigationBarItem(
                                selected = activeTab == NavigationTab.OFFLINE_GUIDE,
                                onClick = { activeTab = NavigationTab.OFFLINE_GUIDE },
                                icon = { Icon(imageVector = Icons.Default.OfflineBolt, contentDescription = "USSD") },
                                label = { Text("Offline") },
                                modifier = Modifier.testTag("nav_offline_tab")
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    // Preserve WebView state across tab switches by using Box helper or conditional layout container
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        // Preserving state and visibility layers
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Home Screen layer
                            if (activeTab == NavigationTab.HOME) {
                                HomeScreen(
                                    viewModel = viewModel,
                                    onNavigateToBrowser = { selectedUrl ->
                                        viewModel.loadUrl(selectedUrl)
                                        activeTab = NavigationTab.BROWSER
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            // WebView Browser layer - always constructed but hidden if not selected to avoid state loss!
                            // This ensures the page stays loaded and user is not reset!
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .offset(y = if (activeTab == NavigationTab.BROWSER) 0.dp else (10000).dp)
                            ) {
                                BrowserScreen(
                                    viewModel = viewModel,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            // Bookmarks Screen layer
                            if (activeTab == NavigationTab.BOOKMARKS) {
                                BookmarksScreen(
                                    viewModel = viewModel,
                                    onNavigateToBrowser = { selectedUrl ->
                                        viewModel.loadUrl(selectedUrl)
                                        activeTab = NavigationTab.BROWSER
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            // Offline guide screen layer
                            if (activeTab == NavigationTab.OFFLINE_GUIDE) {
                                SmsGuideScreen(
                                    viewModel = viewModel,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
