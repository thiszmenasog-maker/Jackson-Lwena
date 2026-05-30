package com.example.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Bookmark
import com.example.data.BookmarkRepository
import com.example.data.DownloadedPaper
import com.example.data.User
import com.example.data.AppNotification
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

// Data class representing a major Tanzanian academic/recruitment agency portal
data class TanzaniaPortal(
    val name: String,
    val fullname: String,
    val websiteUrl: String,
    val description: String,
    val category: String, // MATOKEO, JOBS, SELECTIONS, LOANS, REGISTRATION, OTHER
    val primaryColor: Long, // Hex color for the portal's card
    val isOfficial: Boolean = true
)

class PortalViewModel(private val repository: BookmarkRepository) : ViewModel() {

    // Main navigation and URL selection states
    private val _currentUrl = MutableStateFlow("https://elimupdf.co.tz")
    val currentUrl: StateFlow<String> = _currentUrl.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Shared Flow for trigger events (like browser navigations or toast messages)
    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    // Observable bookmarks list
    val bookmarks: StateFlow<List<Bookmark>> = repository.allBookmarks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val preConfiguredPortals = listOf(
        TanzaniaPortal(
            name = "ElimuPDF",
            fullname = "ElimuPDF Portal - Tanzanian Educational Center",
            websiteUrl = "https://elimupdf.co.tz",
            description = "Download past papers, check NECTA results (Matokeo), search for selections and public job announcements offline or online.",
            category = "ELIMUPDF",
            primaryColor = 0xFF1E3A8A // Deep Sapphire Blue
        ),
        TanzaniaPortal(
            name = "NECTA",
            fullname = "National Examinations Council of Tanzania",
            websiteUrl = "https://www.necta.go.tz",
            description = "Official national exams hub (CSEE, ACSEE, SFNA, PSLE, QT). Access academic results and announcements directly.",
            category = "MATOKEO",
            primaryColor = 0xFF047857 // Emerald Green
        ),
        TanzaniaPortal(
            name = "AJIRA PORTAL",
            fullname = "Public Service Recruitment Secretariat (PSRS)",
            websiteUrl = "https://portal.ajira.go.tz",
            description = "Tanzania's official employment gateway. Apply for government jobs, check job posts, interview calls, and placement lists.",
            category = "JOBS",
            primaryColor = 0xFFB45309 // Amber Gold
        ),
        TanzaniaPortal(
            name = "TAMISEMI",
            fullname = "President's Office - Regional Admin and Local Govt",
            websiteUrl = "https://www.tamisemi.go.tz",
            description = "School selections (Form One, Form Five, College Selections), local government employment and regional administrative updates.",
            category = "SELECTIONS",
            primaryColor = 0xFF0284C7 // Light Sky Blue
        ),
        TanzaniaPortal(
            name = "HESLB",
            fullname = "Higher Education Students' Loans Board",
            websiteUrl = "https://olas.heslb.go.tz",
            description = "Check university loan allocations, apply for loan support, check loan repayment status, and read official guidelines.",
            category = "LOANS",
            primaryColor = 0xFF7C3AED // Rich Purple
        ),
        TanzaniaPortal(
            name = "TCU",
            fullname = "Tanzania Commission for Universities",
            websiteUrl = "https://www.tcu.go.tz",
            description = "University coordination, quality assurance, admissions requirements, and joint admission systems for higher education.",
            category = "SELECTIONS",
            primaryColor = 0xFFDC2626 // Red
        ),
        TanzaniaPortal(
            name = "RITA",
            fullname = "Registration Insolvency and Trusteeship Agency",
            websiteUrl = "https://www.rita.go.tz",
            description = "Verification and issuance of birth certificates required for HESLB loan applications, TCU admission packages, and official documentations.",
            category = "REGISTRATION",
            primaryColor = 0xFF0EA5E9 // Turquoise
        ),
        TanzaniaPortal(
            name = "NACTE",
            fullname = "National Council for Technical & Vocational Education",
            websiteUrl = "https://nacte.go.tz",
            description = "Technical and vocational college admissions, certifications, curriculum standards, and diploma/certificate resources.",
            category = "SELECTIONS",
            primaryColor = 0xFF0D9488 // Teal
        )
    )

    // SMS guides to help students fetch results or loan status offline!
    val smsGuides = listOf(
        SmsGuideItem(
            title = "Check NECTA Results via Phone",
            operator = "Airtel / Vodacom / Tigo / Halotel",
            code = "*152*17#",
            steps = "Dial *152*17# -> Select 'Education' (Elimu) -> Select 'NECTA' -> Select Exam Type -> Enter your Candidate Number -> Get your results instantly via SMS (charges apply)."
        ),
        SmsGuideItem(
            title = "Check Government Salary/Ajira Status",
            operator = "All Mobile Networks",
            code = "*152*00#",
            steps = "Dial *152*00# -> Select 'Utumishi' (Public Service) -> Follow instruction screens to verify application status."
        ),
        SmsGuideItem(
            title = "Verify Birth Certificate with RITA for Loans",
            operator = "All Mobile Networks",
            code = "*152*05#",
            steps = "Dial *152*05# -> Follow prompts to verify birth registration details before applying to HESLB loan system."
        )
    )

    // --- 1. DOWNLOADED PAPERS STATE & BUSINESS ---
    val downloadedPapers: StateFlow<List<DownloadedPaper>> = repository.allDownloadedPapers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun isPaperDownloadedFlow(url: String) = repository.isPaperDownloadedFlow(url)

    fun downloadMockPaper(context: Context, title: String, subject: String, year: String, level: String, fileUrl: String) {
        viewModelScope.launch {
            try {
                if (repository.isPaperDownloaded(fileUrl)) {
                    _toastMessage.emit("Paper already downloaded offline!")
                    return@launch
                }

                val folder = File(context.filesDir, "downloads")
                if (!folder.exists()) {
                    folder.mkdirs()
                }

                val sanitizeTitle = title.replace(Regex("[^a-zA-Z0-9]"), "_") + ".pdf"
                val file = File(folder, sanitizeTitle)
                
                FileOutputStream(file).use { out ->
                    out.write(
                        """
                        ELIMUPDF CO.TZ - OFFLINE PAST PAPER SYSTEM
                        ===========================================
                        Title: $title
                        Subject: $subject
                        Year: $year
                        Level: $level
                        Source: $fileUrl
                        
                        This past paper has been compiled and downloaded for complete offline viewing via the ElimuPDF Portal Android app.
                        
                        PRACTICE QUESTIONS:
                        1. Explain the primary roles of NECTA and TCU in the Tanzanian education system.
                        2. How has the digital centralization of Ajiraportal improved career procurement processing?
                        3. Calculate student loans allocation trends based on official HESLB criteria formulas.
                        
                        [End of resources database. All rights reserved ElimuPDF.]
                        """.trimIndent().toByteArray()
                    )
                }

                val sizeInKb = (file.length() / 1024.0)
                val sizeStr = String.format("%.1f KB", sizeInKb)

                val paper = DownloadedPaper(
                    title = title,
                    subject = subject,
                    year = year,
                    level = level,
                    fileUrl = fileUrl,
                    localFilePath = file.absolutePath,
                    fileSize = sizeStr
                )

                repository.insertDownloadedPaper(paper)
                _toastMessage.emit("Downloaded '$subject $year' successfully for offline viewing!")
            } catch (e: Exception) {
                _toastMessage.emit("Download failed: ${e.localizedMessage}")
            }
        }
    }

    fun deleteDownloadedPaper(paper: DownloadedPaper) {
        viewModelScope.launch {
            try {
                val file = File(paper.localFilePath)
                if (file.exists()) {
                    file.delete()
                }
                repository.deleteDownloadedPaper(paper)
                _toastMessage.emit("Deleted downloaded paper from offline storage")
            } catch (e: Exception) {
                _toastMessage.emit("Failed to delete offline file: ${e.localizedMessage}")
            }
        }
    }

    // --- 2. USER AUTHENTICATION STATE & LOGIC ---
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _authStateError = MutableSharedFlow<String>()
    val authStateError: SharedFlow<String> = _authStateError.asSharedFlow()

    fun signUp(name: String, email: String, passwordHash: String) {
        viewModelScope.launch {
            if (name.isBlank() || email.isBlank() || passwordHash.isBlank()) {
                _authStateError.emit("All registration fields are required.")
                return@launch
            }
            if (repository.userExists(email)) {
                _authStateError.emit("An account with this email already exists.")
                return@launch
            }
            val newUser = User(email = email, name = name, passwordHash = passwordHash)
            repository.registerUser(newUser)
            _currentUser.value = newUser
            _toastMessage.emit("Account registered successfully! Welcome, $name!")
        }
    }

    fun logIn(email: String, passwordHash: String) {
        viewModelScope.launch {
            if (email.isBlank() || passwordHash.isBlank()) {
                _authStateError.emit("Email and password fields are required.")
                return@launch
            }
            val user = repository.getUserByEmail(email)
            if (user == null) {
                _authStateError.emit("No account found with this email.")
                return@launch
            }
            if (user.passwordHash != passwordHash) {
                _authStateError.emit("Incorrect password.")
                return@launch
            }
            _currentUser.value = user
            _toastMessage.emit("Logged in successfully! Welcome back, ${user.name}!")
        }
    }

    fun logOut() {
        viewModelScope.launch {
            _currentUser.value = null
            _toastMessage.emit("Logged out securely")
        }
    }

    // --- 3. PUSH NOTIFICATIONS SUBSCRIPTIONS & ANNOUNCEMENT FEED ---
    val notifications: StateFlow<List<AppNotification>> = repository.allNotifications
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val prefNecta = MutableStateFlow(true)
    val prefAjira = MutableStateFlow(true)
    val prefTcu = MutableStateFlow(true)
    val prefHeslb = MutableStateFlow(true)
    val prefTamisemi = MutableStateFlow(true)

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "ElimuPDF Announcements"
            val descriptionText = "Get live updates on results, jobs, selections & loan details"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("elimupdf_alerts", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun triggerLocalPushNotification(
        context: Context,
        title: String,
        body: String,
        type: String,
        authority: String,
        url: String
    ) {
        viewModelScope.launch {
            val shouldNotify = when (authority.uppercase()) {
                "NECTA" -> prefNecta.value
                "AJIRAPORTAL" -> prefAjira.value
                "TCU", "NACTE" -> prefTcu.value
                "HESLB" -> prefHeslb.value
                "TAMISEMI" -> prefTamisemi.value
                else -> true
            }

            if (!shouldNotify) {
                _toastMessage.emit("Notification from $authority muted by your preferences.")
                return@launch
            }

            // Record in Room Database history feed
            val notificationEntity = AppNotification(
                title = title,
                body = body,
                type = type,
                authority = authority,
                url = url
            )
            repository.insertNotification(notificationEntity)

            // Trigger system tray popup
            createNotificationChannel(context)

            val intent = Intent(context, com.example.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("LAUNCH_URL", url)
            }
            val pendingIntent: PendingIntent = PendingIntent.getActivity(
                context, 
                System.currentTimeMillis().toInt(), 
                intent, 
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val builder = NotificationCompat.Builder(context, "elimupdf_alerts")
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            try {
                val notificationId = System.currentTimeMillis().toInt()
                val manager = NotificationManagerCompat.from(context)
                manager.notify(notificationId, builder.build())
                _toastMessage.emit("Success: Live notification popped up for $authority!")
            } catch (e: SecurityException) {
                _toastMessage.emit("Permission model: Post Notifications permission required.")
            } catch (e: Exception) {
                _toastMessage.emit("Failed to fire push notice: ${e.localizedMessage}")
            }
        }
    }

    fun markNotificationAsRead(id: Int) {
        viewModelScope.launch {
            repository.markNotificationAsRead(id)
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead()
            _toastMessage.emit("All notifications marked as read")
        }
    }

    fun deleteNotification(notification: AppNotification) {
        viewModelScope.launch {
            repository.deleteNotification(notification)
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            repository.clearAllNotifications()
            _toastMessage.emit("Notification history cleared")
        }
    }

    fun loadUrl(url: String) {
        _currentUrl.value = url
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleBookmark(title: String, url: String, category: String) {
        viewModelScope.launch {
            if (repository.isBookmarked(url)) {
                repository.deleteBookmarkByUrl(url)
                _toastMessage.emit("Removed from bookmarks")
            } else {
                repository.insertBookmark(
                    Bookmark(
                        title = title,
                        url = url,
                        category = category,
                        isCustom = true
                    )
                )
                _toastMessage.emit("Added to bookmarks!")
            }
        }
    }

    fun isBookmarkedFlow(url: String) = repository.isBookmarkedFlow(url)
}

data class SmsGuideItem(
    val title: String,
    val operator: String,
    val code: String,
    val steps: String
)

class PortalViewModelFactory(private val repository: BookmarkRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PortalViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PortalViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
