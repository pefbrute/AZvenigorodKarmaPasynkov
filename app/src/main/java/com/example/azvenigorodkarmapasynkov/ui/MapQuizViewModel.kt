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
import com.example.azvenigorodkarmapasynkov.util.GeometryUtils
import kotlinx.coroutines.flow.first
import kotlin.math.max
import org.osmdroid.util.GeoPoint

enum class GameMode {
    MENU,
    MAP_QUIZ,  // Includes SHOW_NAME and NAME_POKE
    IMAGE_QUIZ // Only IMAGE_GUESS
}

enum class QuizMode {
    SHOW_NAME, // Map centers, User chooses name
    NAME_POKE, // App gives name, User clicks map
    IMAGE_GUESS // Show image, User chooses name
}

sealed class QuizState {
    object Loading : QuizState()
    data class Question(
        val item: QuizItem, 
        val mode: QuizMode, 
        val options: List<String> = emptyList(),
        val initialZoom: Float = 15f
    ) : QuizState()
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

    private val _gameMode = MutableStateFlow(GameMode.MENU)
    val gameMode: StateFlow<GameMode> = _gameMode.asStateFlow()

    private var currentQuizItem: QuizItem? = null
    
    init {
        // Observer for initial data load or if DB was empty
        // We can't really "collect" properly without a mode, so we just check if any items exist
        viewModelScope.launch {
            repository.getAllItems().collect { items ->
                if (items.isEmpty() && currentQuizItem == null) {
                    _uiState.value = QuizState.Empty
                } else if (items.isNotEmpty() && currentQuizItem == null) {
                    _uiState.value = QuizState.Empty // Waiting for mode selection
                }
            }
        }
    }

    fun startGame(mode: GameMode) {
        _gameMode.value = mode
        loadNextQuestion()
    }

    fun backToMenu() {
        _gameMode.value = GameMode.MENU
        currentQuizItem = null
        _uiState.value = QuizState.Empty
    }

    fun loadNextQuestion() {
        val currentMode = _gameMode.value
        if (currentMode == GameMode.MENU) return

        viewModelScope.launch {
            val dueList = repository.getDueItems(currentMode).first() // Get current snapshot
            
            // Filter based on mode
            val filteredList = when (currentMode) {
                GameMode.IMAGE_QUIZ -> dueList.filter { it.imageName != null }
                else -> dueList
            }
            
            android.util.Log.d("MapQuizViewModel", "Due Items: ${dueList.size}, Filtered For Mode $currentMode: ${filteredList.size}")

            if (filteredList.isNotEmpty()) {
                val item = filteredList.random()
                currentQuizItem = item
                
                // Determine QuizMode based on GameMode
                val mode = if (currentMode == GameMode.IMAGE_QUIZ) {
                    QuizMode.IMAGE_GUESS
                } else {
                    // MAP_QUIZ: Random between SHOW_NAME and NAME_POKE
                    // Exclude IMAGE_GUESS here to keep modes distinct
                    if (Math.random() > 0.5) QuizMode.SHOW_NAME else QuizMode.NAME_POKE
                }
                
                // Use minZoom for context if available, else default
                val zoom = if (item.minZoom > 0) item.minZoom else 15f
                
                if (mode == QuizMode.SHOW_NAME || mode == QuizMode.IMAGE_GUESS) {
                     val distractors = repository.getDistractors(item)
                     val options = (distractors + item.name).shuffled()
                     _uiState.value = QuizState.Question(item, mode, options, initialZoom = zoom)
                } else {
                    _uiState.value = QuizState.Question(item, mode, initialZoom = zoom)
                }
            } else {
                _uiState.value = QuizState.Empty
            }
        }
    }

    fun submitAnswer(selectedName: String) {
        val item = currentQuizItem ?: return
        val success = item.name == selectedName
        val quality = if (success) 5 else 0
        
        viewModelScope.launch {
            repository.processReview(item, quality, _gameMode.value)
            
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
        
        // Adaptive Radius Logic
        // Base start: item.baseRadius. As mastery increases (successfulReviews), radius decreases.
        // E.g. 5 successful reviews -> 150 - 100 = 50m.
        val baseRadius = item.baseRadius.toDouble()
        val masteryReduction = (item.successfulReviewsMap * 20.0).coerceAtMost(baseRadius * 0.8) // Don't reduce to 0
        val allowedRadius = max(30.0, baseRadius - masteryReduction)

        val distance = GeometryUtils.calculateDistanceToItem(geoPoint, item)
        
        val success = distance < allowedRadius 
        // Quality based on closeness relative to allowed radius
        val quality = if (success) 5 else if (distance < allowedRadius * 2.5) 2 else 0

         viewModelScope.launch {
             repository.processReview(item, quality, _gameMode.value)
             
             _uiState.value = QuizState.Result(
                 item = item,
                 success = success,
                 correctLocation = GeoPoint(item.latitude, item.longitude),
                 userLocation = geoPoint,
                 distance = distance,
                 message = "Distance: ${distance.toInt()}m (Limit: ${allowedRadius.toInt()}m). ${if (success) "Great!" else "Too far!"}"
             )
         }
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
