package com.example.data

import kotlinx.coroutines.flow.Flow

class BookmarkRepository(
    private val bookmarkDao: BookmarkDao,
    private val downloadedPaperDao: DownloadedPaperDao,
    private val userDao: UserDao,
    private val appNotificationDao: AppNotificationDao
) {
    // --- BOOKMARKS ---
    val allBookmarks: Flow<List<Bookmark>> = bookmarkDao.getAllBookmarks()

    suspend fun insertBookmark(bookmark: Bookmark) {
        bookmarkDao.insertBookmark(bookmark)
    }

    suspend fun deleteBookmark(bookmark: Bookmark) {
        bookmarkDao.deleteBookmark(bookmark)
    }

    suspend fun deleteBookmarkByUrl(url: String) {
        bookmarkDao.deleteBookmarkByUrl(url)
    }

    fun isBookmarkedFlow(url: String): Flow<Boolean> {
        return bookmarkDao.isBookmarkedFlow(url)
    }

    suspend fun isBookmarked(url: String): Boolean {
        return bookmarkDao.isBookmarked(url)
    }

    // --- DOWNLOADED PAPERS ---
    val allDownloadedPapers: Flow<List<DownloadedPaper>> = downloadedPaperDao.getAllDownloadedPapers()

    suspend fun insertDownloadedPaper(paper: DownloadedPaper) {
        downloadedPaperDao.insertDownloadedPaper(paper)
    }

    suspend fun deleteDownloadedPaper(paper: DownloadedPaper) {
        downloadedPaperDao.deleteDownloadedPaper(paper)
    }

    suspend fun deleteDownloadedPaperByUrl(url: String) {
        downloadedPaperDao.deleteByUrl(url)
    }

    suspend fun isPaperDownloaded(url: String): Boolean {
        return downloadedPaperDao.isPaperDownloaded(url)
    }

    fun isPaperDownloadedFlow(url: String): Flow<Boolean> {
        return downloadedPaperDao.isPaperDownloadedFlow(url)
    }

    // --- USER AUTHENTICATION ---
    suspend fun getUserByEmail(email: String): User? {
        return userDao.getUserByEmail(email)
    }

    suspend fun registerUser(user: User) {
        userDao.registerUser(user)
    }

    suspend fun userExists(email: String): Boolean {
        return userDao.userExists(email)
    }

    // --- NOTIFICATIONS ---
    val allNotifications: Flow<List<AppNotification>> = appNotificationDao.getAllNotifications()

    suspend fun insertNotification(notification: AppNotification) {
        appNotificationDao.insertNotification(notification)
    }

    suspend fun markNotificationAsRead(id: Int) {
        appNotificationDao.markAsRead(id)
    }

    suspend fun markAllNotificationsAsRead() {
        appNotificationDao.markAllAsRead()
    }

    suspend fun deleteNotification(notification: AppNotification) {
        appNotificationDao.deleteNotification(notification)
    }

    suspend fun clearAllNotifications() {
        appNotificationDao.clearAllNotifications()
    }
}
