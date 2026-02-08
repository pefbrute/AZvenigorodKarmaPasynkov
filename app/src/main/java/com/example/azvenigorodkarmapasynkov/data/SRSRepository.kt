package com.example.azvenigorodkarmapasynkov.data

import kotlinx.coroutines.flow.Flow
import java.util.Calendar

import kotlin.math.max
import kotlin.math.roundToInt

import com.example.azvenigorodkarmapasynkov.ui.GameMode

class SRSRepository(private val quizDao: QuizDao) {


    fun getDueItems(mode: GameMode): Flow<List<QuizItem>> {
        val currentTime = System.currentTimeMillis()
        return when (mode) {
             GameMode.IMAGE_QUIZ -> quizDao.getItemsDueForImageReview(currentTime)
             else -> quizDao.getItemsDueForMapReview(currentTime)
        }
    }

    fun getAllItems(): Flow<List<QuizItem>> {
        return quizDao.getAllItems()
    }
    
    suspend fun isEmpty(): Boolean {
        return quizDao.getItemCount() == 0
    }

    suspend fun getItemById(id: String): QuizItem? {
        return quizDao.getItemById(id)
    }
    
    suspend fun getDistractors(item: QuizItem, count: Int = 3): List<String> {
        val range = 0.05 // Roughly 5km buffer
        val nearby = quizDao.getNearbyItems(
            type = item.type,
            excludeId = item.id,
            minLat = item.latitude - range,
            maxLat = item.latitude + range,
            minLon = item.longitude - range,
            maxLon = item.longitude + range,
            limit = count * 3
        )
        
        // Filter out same-named items (e.g. other segments of the same street)
        val filteredNearby = nearby.filter { it.name != item.name }
        
        return filteredNearby.map { it.name }.distinct().shuffled().take(count)
    }

    suspend fun addItem(item: QuizItem) {
        quizDao.insertItem(item)
    }

    suspend fun updateItem(item: QuizItem) {
        quizDao.updateItem(item)
    }
    
    suspend fun addItems(items: List<QuizItem>) {
        quizDao.insertItems(items)
    }

    /**
     * Updates the item based on the user's performance using a simplified SM-2 algorithm.
     * @param item The quiz item.
     * @param quality 0-5 rating. (0: blackout, 5: perfect)
     */
    suspend fun processReview(item: QuizItem, quality: Int, mode: GameMode) {
        // SM-2 Algorithm
        // q: 0-5
        // EF' = EF + (0.1 - (5-q)*(0.08 + (5-q)*0.02))
        // If q < 3, start repetitions over from count 1.
        
        var interval = if (mode == GameMode.IMAGE_QUIZ) item.intervalImage else item.intervalMap
        var easeFactor = if (mode == GameMode.IMAGE_QUIZ) item.easeFactorImage else item.easeFactorMap
        var successfulReviews = if (mode == GameMode.IMAGE_QUIZ) item.successfulReviewsImage else item.successfulReviewsMap
        
        var newInterval: Int
        var newEF = easeFactor
        var newSuccessfulReviews = successfulReviews

        if (quality >= 3) {
            newSuccessfulReviews += 1
            if (newSuccessfulReviews == 1) {
                newInterval = 1
            } else if (newSuccessfulReviews == 2) {
                newInterval = 6
            } else {
                newInterval = (interval * newEF).roundToInt()
            }
            
            newEF = newEF + (0.1f - (5 - quality) * (0.08f + (5 - quality) * 0.02f))
            if (newEF < 1.3f) newEF = 1.3f
        } else {
            newSuccessfulReviews = 0
            newInterval = 1
        }

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, newInterval)
        val newNextReviewDate = calendar.timeInMillis

        val updatedItem = if (mode == GameMode.IMAGE_QUIZ) {
             item.copy(
                intervalImage = newInterval,
                easeFactorImage = newEF,
                successfulReviewsImage = newSuccessfulReviews,
                nextReviewDateImage = newNextReviewDate
            )
        } else {
             item.copy(
                intervalMap = newInterval,
                easeFactorMap = newEF,
                successfulReviewsMap = newSuccessfulReviews,
                nextReviewDateMap = newNextReviewDate
            )
        }

        quizDao.updateItem(updatedItem)
    }
}
