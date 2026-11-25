package com.gh.healthapp.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "chat_message")
data class ChatMessageEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val role: String, val text: String, val time: Long, val sourcesJson: String? = null)

@Dao
interface ChatDao {
    @Insert suspend fun insert(message: ChatMessageEntity): Long
    @Query("SELECT * FROM chat_message ORDER BY id ASC") fun messages(): Flow<List<ChatMessageEntity>>
    @Query("SELECT * FROM chat_message ORDER BY id DESC LIMIT :limit") suspend fun recent(limit: Int): List<ChatMessageEntity>
    @Query("DELETE FROM chat_message") suspend fun clear()
}
