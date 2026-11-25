package com.gh.healthapp.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction

@Entity(tableName = "kb_chunk")
data class KbChunkEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val docId: String, val title: String, val heading: String, val text: String, val source: String, val sourceUrl: String?, val vector: ByteArray, val contentHash: String)

@Dao
interface KbDao {
    @Query("SELECT * FROM kb_chunk ORDER BY id") suspend fun all(): List<KbChunkEntity>
    @Query("SELECT COUNT(*) FROM kb_chunk") suspend fun count(): Int
    @Query("DELETE FROM kb_chunk") suspend fun deleteAll()
    @Insert suspend fun insertAll(chunks: List<KbChunkEntity>)
    @Transaction suspend fun replaceAll(chunks: List<KbChunkEntity>) { deleteAll(); insertAll(chunks) }
}
