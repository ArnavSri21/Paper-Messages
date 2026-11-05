package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

@Entity(tableName = "local_message_search_index")
data class LocalIndexedMessage(
    @PrimaryKey val messageId: String,
    val userId: String,
    val conversationId: String,
    val conversationTitle: String,
    val senderName: String,
    val topic: String,
    val decryptedPlaintext: String,
    val updatedAtEpochMs: Long
)

@Dao
interface LocalSearchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertIndexedMessage(item: LocalIndexedMessage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertIndexedMessages(items: List<LocalIndexedMessage>)

    @Query(
        """
        SELECT * FROM local_message_search_index 
        WHERE userId = :userId 
          AND (decryptedPlaintext LIKE '%' || :query || '%' 
               OR senderName LIKE '%' || :query || '%' 
               OR conversationTitle LIKE '%' || :query || '%'
               OR topic LIKE '%' || :query || '%')
        ORDER BY updatedAtEpochMs DESC
        LIMIT 50
        """
    )
    suspend fun searchDecryptedMessages(userId: String, query: String): List<LocalIndexedMessage>

    @Query("DELETE FROM local_message_search_index WHERE userId = :userId")
    suspend fun clearUserIndex(userId: String)
}

@Database(entities = [LocalIndexedMessage::class], version = 1, exportSchema = false)
abstract class LocalSearchDatabase : RoomDatabase() {
    abstract fun searchDao(): LocalSearchDao

    companion object {
        @Volatile
        private var INSTANCE: LocalSearchDatabase? = null

        fun getInstance(context: Context): LocalSearchDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LocalSearchDatabase::class.java,
                    "papermessages_encrypted_index.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
