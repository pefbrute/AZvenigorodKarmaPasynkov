package com.example.azvenigorodkarmapasynkov.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface QuizDao {
    @Query("SELECT * FROM quiz_items")
    fun getAllItems(): Flow<List<QuizItem>>

    @Query("SELECT * FROM quiz_items WHERE nextReviewDate <= :currentTime")
    fun getItemsDueForReview(currentTime: Long): Flow<List<QuizItem>>

    @Query("SELECT COUNT(*) FROM quiz_items")
    suspend fun getItemCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: QuizItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<QuizItem>)

    @Update
    suspend fun updateItem(item: QuizItem)
    
    @Query("SELECT * FROM quiz_items WHERE id = :id")
    suspend fun getItemById(id: Long): QuizItem?

    @Query("SELECT * FROM quiz_items WHERE type = :type AND id != :excludeId AND latitude BETWEEN :minLat AND :maxLat AND longitude BETWEEN :minLon AND :maxLon ORDER BY RANDOM() LIMIT :limit")
    suspend fun getNearbyItems(
        type: QuizType, 
        excludeId: Long, 
        minLat: Double, 
        maxLat: Double, 
        minLon: Double, 
        maxLon: Double, 
        limit: Int
    ): List<QuizItem>
}

@Database(entities = [QuizItem::class], version = 3, exportSchema = false)
abstract class QuizDatabase : RoomDatabase() {
    abstract fun quizDao(): QuizDao

    companion object {
        @Volatile
        private var INSTANCE: QuizDatabase? = null

        fun getDatabase(context: Context): QuizDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    QuizDatabase::class.java,
                    "quiz_database"
                )
                .fallbackToDestructiveMigration()
                // Pre-populate logic could go here
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
