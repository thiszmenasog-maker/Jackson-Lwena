package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadedPaperDao {
    @Query("SELECT * FROM downloaded_papers ORDER BY downloadTime DESC")
    fun getAllDownloadedPapers(): Flow<List<DownloadedPaper>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownloadedPaper(paper: DownloadedPaper)

    @Delete
    suspend fun deleteDownloadedPaper(paper: DownloadedPaper)

    @Query("DELETE FROM downloaded_papers WHERE fileUrl = :url")
    suspend fun deleteByUrl(url: String)

    @Query("SELECT EXISTS(SELECT * FROM downloaded_papers WHERE fileUrl = :url)")
    suspend fun isPaperDownloaded(url: String): Boolean

    @Query("SELECT EXISTS(SELECT * FROM downloaded_papers WHERE fileUrl = :url)")
    fun isPaperDownloadedFlow(url: String): Flow<Boolean>
}
