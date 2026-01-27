package com.example.azvenigorodkarmapasynkov.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.azvenigorodkarmapasynkov.data.QuizItem
import com.example.azvenigorodkarmapasynkov.data.QuizType
import com.example.azvenigorodkarmapasynkov.data.SRSRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class QuizMode {
    SHOW_NAME, // Map centers, User chooses name
    NAME_POKE // App gives name, User clicks map
}

sealed class QuizState {
    object Loading : QuizState()
    data class Question(val item: QuizItem, val mode: QuizMode, val options: List<String> = emptyList()) : QuizState()
    data class Result(
        val item: QuizItem, 
        val success: Boolean, 
        val correctLocation: GeoPoint, 
        val userLocation: GeoPoint?, 
        val distance: Double,
        val message: String
    ) : QuizState()
    object Empty : QuizState()
}

class MapQuizViewModel(private val repository: SRSRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<QuizState>(QuizState.Loading)
    val uiState: StateFlow<QuizState> = _uiState.asStateFlow()

    private var currentQuizItem: QuizItem? = null
    
    // Temporary list for demo if DB is empty
    private val demoItems = listOf(
        QuizItem(name = "Zvenigorod Kremlin", latitude = 55.7305, longitude = 36.8527, type = QuizType.POINT),
        QuizItem(name = "Savvino-Starozhevsky Monastery", latitude = 55.7288, longitude = 36.8166, type = QuizType.POINT),
        QuizItem(name = "Cultural Center", latitude = 55.7330, longitude = 36.8580, type = QuizType.POINT)
    )

    init {
        loadNextQuestion()
    }

    fun loadNextQuestion() {
        viewModelScope.launch {
            // In a real app, collect from Repository. For now, pick random demo item or mock flow.
            // We'll mimic fetching "Due" items.
            // For MVP, just cycle through demo items or random
            
            val item = demoItems.random()
            currentQuizItem = item
            
            // Randomly choose mode
            val mode = if (Math.random() > 0.5) QuizMode.SHOW_NAME else QuizMode.NAME_POKE
            
            if (mode == QuizMode.SHOW_NAME) {
                 // Generate options
                 val options = (demoItems - item).shuffled().take(3).map { it.name } + item.name
                 _uiState.value = QuizState.Question(item, mode, options.shuffled())
            } else {
                _uiState.value = QuizState.Question(item, mode)
            }
        }
    }

    fun submitAnswer(selectedName: String) {
        val item = currentQuizItem ?: return
        val success = item.name == selectedName
        val quality = if (success) 5 else 0
        
        viewModelScope.launch {
            // Save result (if item was in DB)
            // repository.processReview(item, quality)
            
            _uiState.value = QuizState.Result(
                item = item,
                success = success,
                correctLocation = GeoPoint(item.latitude, item.longitude),
                userLocation = null,
                distance = 0.0,
                message = if (success) "Correct!" else "Incorrect. It was ${item.name}"
            )
        }
    }

    fun submitLocation(geoPoint: GeoPoint) {
        val item = currentQuizItem ?: return
        val correctPoint = GeoPoint(item.latitude, item.longitude)
        val distance = calculateDistanceIds(geoPoint, correctPoint)
        
        // Define tolerance, e.g., 100 meters
        val success = distance < 100 
        val quality = if (success) 5 else if (distance < 500) 3 else 0

         viewModelScope.launch {
            // Save result
             // repository.processReview(item, quality)
             
             _uiState.value = QuizState.Result(
                 item = item,
                 success = success,
                 correctLocation = correctPoint,
                 userLocation = geoPoint,
                 distance = distance,
                 message = "Distance: ${distance.toInt()}m. ${if (success) "Great!" else "Too far!"}"
             )
         }
    }
    
    private fun calculateDistanceIds(p1: GeoPoint, p2: GeoPoint): Double {
        val R = 6371e3 // metres
        val phi1 = p1.latitude * Math.PI / 180
        val phi2 = p2.latitude * Math.PI / 180
        val deltaPhi = (p2.latitude - p1.latitude) * Math.PI / 180
        val deltaLambda = (p2.longitude - p1.longitude) * Math.PI / 180

        val a = sin(deltaPhi / 2) * sin(deltaPhi / 2) +
                cos(phi1) * cos(phi2) *
                sin(deltaLambda / 2) * sin(deltaLambda / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return R * c
    }
}

class MapQuizViewModelFactory(private val repository: SRSRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MapQuizViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MapQuizViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
