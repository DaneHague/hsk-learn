package com.hsklearn.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [FlashcardEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun flashcardDao(): FlashcardDao
}
