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
    val imageName: String? = null, // Name of drawable resource
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

    // SRS fields for Map
    val nextReviewDateMap: Long = 0, // Timestamp
    val intervalMap: Int = 0, // Days
    val easeFactorMap: Float = 2.5f,
    val successfulReviewsMap: Int = 0,

    // SRS fields for Image
    val nextReviewDateImage: Long = 0, // Timestamp
    val intervalImage: Int = 0, // Days
    val easeFactorImage: Float = 2.5f,
    val successfulReviewsImage: Int = 0,
    
    // Config
    val baseRadius: Int = 150
)
