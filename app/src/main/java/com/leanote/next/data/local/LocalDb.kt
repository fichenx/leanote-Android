package com.leanote.next.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "notes",
    indices = [
        Index("accountId"),
        Index("notebookServerId"),
        Index("serverId"),
        Index("isDirty"),
    ],
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val accountId: String,
    val serverId: String = "",
    val notebookServerId: String = "",
    val title: String = "",
    val content: String = "",
    val isMarkdown: Boolean = true,
    val tags: String = "",
    val isTrash: Boolean = false,
    val isBlog: Boolean = false,
    val urlTitle: String = "",
    val usn: Int = 0,
    val createdTime: Long = 0L,
    val updatedTime: Long = 0L,
    val isDirty: Boolean = false,
) {
    val tagList: List<String>
        get() = tags.split(',').map { it.trim() }.filter { it.isNotEmpty() }

    val isLocalOnly: Boolean get() = serverId.isBlank()
}

@Entity(
    tableName = "notebooks",
    indices = [Index(value = ["accountId", "serverId"], unique = true)],
)
data class NotebookEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val accountId: String,
    val serverId: String = "",
    val parentServerId: String = "",
    val title: String = "",
    val seq: Int = 0,
    val usn: Int = 0,
)

@Entity(tableName = "note_images")
data class NoteImageEntity(
    @PrimaryKey val localId: String,
    val noteLocalId: Long,
    val serverId: String? = null,
    val localPath: String,
    val mimeType: String = "image/jpeg",
)

@Entity(tableName = "note_attachments")
data class NoteAttachmentEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val noteLocalId: Long,
    val accountId: String,
    val serverId: String? = null,
    val fileName: String,
    val mimeType: String = "application/octet-stream",
    val size: Long = 0L,
    val localPath: String? = null,
    val createdTime: Long = 0L,
)

@Dao
interface NoteDao {
    @Query(
        """
        SELECT * FROM notes
        WHERE accountId = :accountId AND isTrash = :trash
          AND (:notebookId = '' OR notebookServerId = :notebookId)
        ORDER BY updatedTime DESC
        """
    )
    fun observe(accountId: String, notebookId: String, trash: Boolean = false): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE accountId = :accountId ORDER BY updatedTime DESC")
    fun observeAll(accountId: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE localId = :localId")
    fun observeById(localId: Long): Flow<NoteEntity?>

    @Query("SELECT * FROM notes WHERE localId = :localId")
    suspend fun getById(localId: Long): NoteEntity?

    @Query("SELECT * FROM notes WHERE accountId = :accountId AND serverId = :serverId LIMIT 1")
    suspend fun getByServerId(accountId: String, serverId: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE accountId = :accountId AND isDirty = 1")
    suspend fun getDirty(accountId: String): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE accountId = :accountId AND isTrash = 0 ORDER BY updatedTime DESC LIMIT :limit")
    suspend fun recent(accountId: String, limit: Int): List<NoteEntity>

    @Query("SELECT tags FROM notes WHERE accountId = :accountId AND isTrash = 0 AND tags != ''")
    fun observeTagRows(accountId: String): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM notes WHERE accountId = :accountId AND isTrash = 0")
    fun observeCount(accountId: String): Flow<Int>

    @Upsert
    suspend fun upsert(note: NoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(note: NoteEntity): Long

    @Delete
    suspend fun delete(note: NoteEntity)

    @Query("DELETE FROM notes WHERE accountId = :accountId")
    suspend fun deleteAllOfAccount(accountId: String)

    @Query("UPDATE notes SET isTrash = :trash, isDirty = 1, updatedTime = :time WHERE localId = :localId")
    suspend fun setTrash(localId: Long, trash: Boolean, time: Long)
}

@Dao
interface NotebookDao {
    @Query("SELECT * FROM notebooks WHERE accountId = :accountId ORDER BY seq ASC, title ASC")
    fun observe(accountId: String): Flow<List<NotebookEntity>>

    @Query("SELECT * FROM notebooks WHERE accountId = :accountId ORDER BY seq ASC, title ASC")
    suspend fun getAll(accountId: String): List<NotebookEntity>

    @Query("SELECT * FROM notebooks WHERE accountId = :accountId AND serverId = :serverId LIMIT 1")
    suspend fun getByServerId(accountId: String, serverId: String): NotebookEntity?

    @Query("SELECT * FROM notebooks WHERE accountId = :accountId AND localId = :localId")
    suspend fun getById(accountId: String, localId: Long): NotebookEntity?

    @Upsert
    suspend fun upsert(notebook: NotebookEntity): Long

    @Delete
    suspend fun delete(notebook: NotebookEntity)

    @Query("DELETE FROM notebooks WHERE accountId = :accountId")
    suspend fun deleteAllOfAccount(accountId: String)
}

@Dao
interface ImageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(image: NoteImageEntity)

    @Query("SELECT * FROM note_images WHERE localId = :localId LIMIT 1")
    suspend fun getByLocalId(localId: String): NoteImageEntity?

    @Query("SELECT * FROM note_images WHERE serverId = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: String): NoteImageEntity?

    @Query("SELECT * FROM note_images WHERE noteLocalId = :noteLocalId")
    suspend fun listOfNote(noteLocalId: Long): List<NoteImageEntity>

    @Query("SELECT * FROM note_images WHERE localId IN (:ids)")
    suspend fun listByLocalIds(ids: List<String>): List<NoteImageEntity>

    @Query("UPDATE note_images SET serverId = :serverId WHERE localId = :localId")
    suspend fun updateServerId(localId: String, serverId: String)

    @Query("DELETE FROM note_images WHERE noteLocalId = :noteLocalId")
    suspend fun deleteOfNote(noteLocalId: Long)
}

@Dao
interface AttachmentDao {
    @Query("SELECT * FROM note_attachments WHERE noteLocalId = :noteLocalId ORDER BY createdTime DESC, localId DESC")
    fun observe(noteLocalId: Long): Flow<List<NoteAttachmentEntity>>

    @Query("SELECT * FROM note_attachments WHERE noteLocalId = :noteLocalId ORDER BY createdTime DESC, localId DESC")
    suspend fun listOfNote(noteLocalId: Long): List<NoteAttachmentEntity>

    @Upsert
    suspend fun upsert(attachment: NoteAttachmentEntity): Long

    @Delete
    suspend fun delete(attachment: NoteAttachmentEntity)

    @Query("SELECT * FROM note_attachments WHERE serverId = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: String): NoteAttachmentEntity?
}

@Database(
    entities = [NoteEntity::class, NotebookEntity::class, NoteImageEntity::class, NoteAttachmentEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun notebookDao(): NotebookDao
    abstract fun imageDao(): ImageDao
    abstract fun attachmentDao(): AttachmentDao
}
