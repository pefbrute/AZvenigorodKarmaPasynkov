package com.example.azvenigorodkarmapasynkov.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

enum class QuizType {
    POINT, LINE, POLYGON
}

@Entity(tableName = "quiz_items")
@Serializable
data class QuizItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val latitude: Double, // Center for point, or label position
    val longitude: Double,
    val type: QuizType = QuizType.POINT,
    
    // Geometry & Viewport
    val geometryData: String = "", // JSON list of coordinates e.g. "[[lat,lon],[lat,lon]]"
    val minZoom: Float = 14f,
    val maxZoom: Float = 18f,

    // Bounding Box for fast queries
    val bboxMinLat: Double = 0.0,
    val bboxMaxLat: Double = 0.0,
    val bboxMinLon: Double = 0.0,
    val bboxMaxLon: Double = 0.0,

    // SRS fields
    val nextReviewDate: Long = 0, // Timestamp
    val interval: Int = 0, // Days
    val easeFactor: Float = 2.5f,
    val successfulReviews: Int = 0,
    
    // Config
    val baseRadius: Int = 150
)
