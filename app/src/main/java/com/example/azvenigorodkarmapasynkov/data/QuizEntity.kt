package com.example.azvenigorodkarmapasynkov.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

enum class QuizType {
    POINT, LINE
}

@Entity(tableName = "quiz_items")
@Serializable
data class QuizItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val latitude: Double, // Center for point, or starting point for line
    val longitude: Double,
    val type: QuizType = QuizType.POINT,
    // SRS fields
    val nextReviewDate: Long = 0, // Timestamp
    val interval: Int = 0, // Days
    val easeFactor: Float = 2.5f,
    val successfulReviews: Int = 0
)
