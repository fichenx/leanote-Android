package com.leanote.next.di

import android.content.Context
import androidx.room.Room
import com.leanote.next.data.local.AppDatabase
import com.leanote.next.data.local.ImageDao
import com.leanote.next.data.local.NoteDao
import com.leanote.next.data.local.NotebookDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "leanote.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideNoteDao(db: AppDatabase): NoteDao = db.noteDao()

    @Provides
    fun provideNotebookDao(db: AppDatabase): NotebookDao = db.notebookDao()

    @Provides
    fun provideImageDao(db: AppDatabase): ImageDao = db.imageDao()
}
