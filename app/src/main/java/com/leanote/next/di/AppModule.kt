package com.leanote.next.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.leanote.next.data.local.AppDatabase
import com.leanote.next.data.local.AttachmentDao
import com.leanote.next.data.local.ImageDao
import com.leanote.next.data.local.NoteDao
import com.leanote.next.data.local.NotebookDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `note_attachments` (
                `localId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `noteLocalId` INTEGER NOT NULL,
                `accountId` TEXT NOT NULL,
                `serverId` TEXT,
                `fileName` TEXT NOT NULL,
                `mimeType` TEXT NOT NULL,
                `size` INTEGER NOT NULL,
                `localPath` TEXT,
                `createdTime` INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "leanote.db")
            .addMigrations(MIGRATION_1_2)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideNoteDao(db: AppDatabase): NoteDao = db.noteDao()

    @Provides
    fun provideNotebookDao(db: AppDatabase): NotebookDao = db.notebookDao()

    @Provides
    fun provideImageDao(db: AppDatabase): ImageDao = db.imageDao()

    @Provides
    fun provideAttachmentDao(db: AppDatabase): AttachmentDao = db.attachmentDao()
}
