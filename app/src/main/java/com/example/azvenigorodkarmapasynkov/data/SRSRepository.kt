package com.example.azvenigorodkarmapasynkov.data

import kotlinx.coroutines.flow.Flow
import java.util.Calendar

import kotlin.math.max
import kotlin.math.roundToInt

class SRSRepository(private val quizDao: QuizDao) {


    fun getDueItems(): Flow<List<QuizItem>> {
        return quizDao.getItemsDueForReview(System.currentTimeMillis())
    }

    fun getAllItems(): Flow<List<QuizItem>> {
        return quizDao.getAllItems()
    }

    suspend fun addItem(item: QuizItem) {
        quizDao.insertItem(item)
    }
    
    suspend fun addItems(items: List<QuizItem>) {
        quizDao.insertItems(items)
    }

    /**
     * Updates the item based on the user's performance using a simplified SM-2 algorithm.
     * @param item The quiz item.
     * @param quality 0-5 rating. (0: blackout, 5: perfect)
     */
    suspend fun processReview(item: QuizItem, quality: Int) {
        // SM-2 Algorithm
        // q: 0-5
        // EF' = EF + (0.1 - (5-q)*(0.08 + (5-q)*0.02))
        // If q < 3, start repetitions over from count 1.
        
        var newInterval: Int
        var newEF = item.easeFactor
        var newSuccessfulReviews = item.successfulReviews

        if (quality >= 3) {
            newSuccessfulReviews += 1
            if (newSuccessfulReviews == 1) {
                newInterval = 1
            } else if (newSuccessfulReviews == 2) {
                newInterval = 6
            } else {
                newInterval = (item.interval * newEF).roundToInt()
            }
            
            newEF = newEF + (0.1f - (5 - quality) * (0.08f + (5 - quality) * 0.02f))
            if (newEF < 1.3f) newEF = 1.3f
        } else {
            newSuccessfulReviews = 0
            newInterval = 1
            // EF stays same or could decrease, standard SM-2 keeps it same or decreases slightly.
            // Let's keep EF same for simplicity on failure to reset chain.
        }

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, newInterval)
        val newNextReviewDate = calendar.timeInMillis

        val updatedItem = item.copy(
            interval = newInterval,
            easeFactor = newEF,
            successfulReviews = newSuccessfulReviews,
            nextReviewDate = newNextReviewDate
        )

        quizDao.updateItem(updatedItem)
    }
}
