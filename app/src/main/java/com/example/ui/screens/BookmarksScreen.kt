package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.Bookmark
import com.example.data.DownloadedPaper
import com.example.ui.PortalViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    viewModel: PortalViewModel,
    onNavigateToBrowser: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val bookmarks by viewModel.bookmarks.collectAsState()
    val downloadedPapers by viewModel.downloadedPapers.collectAsState()
    
    var activeSubTab by remember { mutableStateOf(0) } // 0 = Saved Links, 1 = Offline Library
    var activeViewerPaper by remember { mutableStateOf<DownloadedPaper?>(null) }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                TopAppBar(
                    title = {
                        Text(
                            text = "My Personal Library",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
                
                // Secondary visual switch for links vs offline documents
                TabRow(
                    selectedTabIndex = activeSubTab,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).clip(RoundedCornerShape(8.dp))
                ) {
                    Tab(
                        selected = activeSubTab == 0,
                        onClick = { activeSubTab = 0 },
                        text = { Text("Saved Links (${bookmarks.size})", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.Bookmarks, contentDescription = "Links") }
                    )
                    Tab(
                        selected = activeSubTab == 1,
                        onClick = { activeSubTab = 1 },
                        text = { Text("Offline PDFs (${downloadedPapers.size})", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.DownloadForOffline, contentDescription = "Offline Library") }
                    )
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
            when (activeSubTab) {
                0 -> {
                    // Saved Bookmarked Portals Layout
                    if (bookmarks.isEmpty()) {
                        BookmarksEmptyState(onBrowse = { onNavigateToBrowser("https://elimupdf.co.tz") })
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(bookmarks, key = { it.id }) { bookmark ->
                                BookmarkListItem(
                                    bookmark = bookmark,
                                    onOpen = { onNavigateToBrowser(bookmark.url) },
                                    onDelete = { viewModel.toggleBookmark(bookmark.title, bookmark.url, bookmark.category) }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // Offline Paper Download Manager Layout
                    if (downloadedPapers.isEmpty()) {
                        OfflineDownloadsEmptyState(onBrowse = { onNavigateToBrowser("https://elimupdf.co.tz") })
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(downloadedPapers, key = { it.id }) { paper ->
                                OfflinePaperListItem(
                                    paper = paper,
                                    onRead = { activeViewerPaper = paper },
                                    onDelete = { viewModel.deleteDownloadedPaper(paper) }
                                )
                            }
                        }
                    }
                }
            }

            // Beautiful Embedded PDF Reader overlay when paper is clicked
            activeViewerPaper?.let { paper ->
                OfflinePdfReaderDialog(
                    paper = paper,
                    onDismiss = { activeViewerPaper = null }
                )
            }
        }
    }
}

@Composable
fun BookmarksEmptyState(onBrowse: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.testTag("bookmarks_empty_state")
        ) {
            Icon(
                imageVector = Icons.Default.BookmarkBorder,
                contentDescription = "No Saved Links",
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                modifier = Modifier.size(72.dp)
            )
            Text(
                text = "No saved links yet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "When visiting TAMISEMI, TCU, or NECTA inside our browser portal, click the bookmark ribbon icon at the top of the browser to save portals here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            
            Button(
                onClick = onBrowse,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Public, contentDescription = "Browse")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Surf Educational Portals")
            }
        }
    }
}

@Composable
fun OfflineDownloadsEmptyState(onBrowse: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.testTag("offline_empty_state")
        ) {
            Icon(
                imageVector = Icons.Default.CloudDownload,
                contentDescription = "No Downloads",
                tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                modifier = Modifier.size(72.dp)
            )
            Text(
                text = "Your offline library is empty",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "You can download past examination revision papers, syllabus docs, and TAMISEMI joining instructions directly from inside the browser tab to read them offline later.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            
            Button(
                onClick = onBrowse,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.DownloadForOffline, contentDescription = "Explore past papers")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Search & Download Papers")
            }
        }
    }
}

@Composable
fun OfflinePaperListItem(
    paper: DownloadedPaper,
    onRead: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onRead() }
            .testTag("downloaded_paper_${paper.id}")
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon with background
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.PictureAsPdf,
                    contentDescription = "PDF Icon",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = paper.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${paper.level} (${paper.year})",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.outlineVariant, CircleShape)
                            .size(3.dp)
                    )
                    
                    Text(
                        text = paper.fileSize,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onRead) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = "Read Offline",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete File",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

// Gorgeous High-Fidelity Custom Layout Offline PDF Viewer dialog
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflinePdfReaderDialog(
    paper: DownloadedPaper,
    onDismiss: () -> Unit
) {
    var fileContent by remember { mutableStateOf("Reading file contents offline...") }

    // Read simulated local file safe on disk
    LaunchedEffect(paper.localFilePath) {
        try {
            val file = File(paper.localFilePath)
            if (file.exists()) {
                fileContent = file.readText()
            } else {
                fileContent = "Error: Local file link is missing or broken. Please re-download this resource."
            }
        } catch (e: Exception) {
            fileContent = "Failure reading file: ${e.localizedMessage}"
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF1E293B) // Premium dark reading background slate!
        ) {
            Scaffold(
                containerColor = Color(0xFF0F172A),
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = paper.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Offline Mode - Internal Storage Reader",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close PDF System Viewer",
                                    tint = Color.White
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFF1E293B)
                        ),
                        actions = {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF047857), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "SECURE PDF",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    )
                },
                modifier = Modifier.fillMaxSize()
            ) { insets ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(insets)
                        .background(Color(0xFF0F172A))
                ) {
                    // Document statistics top bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E293B))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Feed, "Pages", tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                            Text("Document Pages: 1/1 (Full Text)", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                        
                        Text(
                            text = "Size: ${paper.fileSize}",
                            color = Color(0xFFFBBF24),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    // Content Canvas (Styled to look like a crisp physical printed exam transcript paper!)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(18.dp)
                        ) {
                            // Tanzania Flag ribbon indicator on pdf
                            Row(modifier = Modifier.fillMaxWidth().height(4.dp)) {
                                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF10B981)))
                                Box(modifier = Modifier.weight(0.2f).fillMaxHeight().background(Color(0xFFFBBF24)))
                                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF2563EB)))
                            }
                            
                            Spacer(modifier = Modifier.height(14.dp))
                            
                            // Actual layout scrollable body
                            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    item {
                                        Text(
                                            text = fileContent,
                                            color = Color(0xFF1E293B),
                                            lineHeight = 22.sp,
                                            fontSize = 13.sp,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Bottom info bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ElimuPDF Offline past papers are digitally watermarked.",
                            fontSize = 9.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BookmarkListItem(
    bookmark: Bookmark,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onOpen() }
            .testTag("bookmark_item_${bookmark.id}")
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = bookmark.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = bookmark.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = "WEBPAGE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onOpen) {
                    Icon(
                        imageVector = Icons.Default.Launch,
                        contentDescription = "Open Bookmark",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.BookmarkRemove,
                        contentDescription = "Delete Bookmark",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

