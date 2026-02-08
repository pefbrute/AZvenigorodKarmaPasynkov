package com.example.azvenigorodkarmapasynkov.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.azvenigorodkarmapasynkov.map.MapController
import com.example.azvenigorodkarmapasynkov.data.QuizType
import com.example.azvenigorodkarmapasynkov.util.GeometryUtils
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

@Composable
fun MapQuizScreen(viewModel: MapQuizViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val gameMode by viewModel.gameMode.collectAsState()
    val context = LocalContext.current

    if (gameMode == GameMode.MENU) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Zvenigorod Karma Quiz", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { viewModel.startGame(GameMode.MAP_QUIZ) },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Map Quiz (Find location)")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.startGame(GameMode.IMAGE_QUIZ) },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Image Quiz (Guess place)")
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            val currentQuestionState = uiState as? QuizState.Question
        if (currentQuestionState != null && currentQuestionState.mode == QuizMode.IMAGE_GUESS) {
            val item = currentQuestionState.item
            val imageResId = context.resources.getIdentifier(item.imageName, "drawable", context.packageName)
            if (imageResId != 0) {
                Image(
                    painter = painterResource(id = imageResId),
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(modifier = Modifier.fillMaxSize().background(Color.Gray), contentAlignment = Alignment.Center) {
                    Text("Image not found: ${item.imageName}", color = Color.White)
                }
            }
        } else {
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        val controller = MapController(ctx, this)
                        controller.setOnlineMap() // Default for now, can switch to offline if file exists
                        controller.centerMap(55.7288, 36.8166) // Zvenigorod center
                        
                        controller.setOnMapClickListener { geoPoint ->
                            // Only handle clicks if in NAME_POKE mode
                            val currentState = viewModel.uiState.value
                            if (currentState is QuizState.Question && currentState.mode == QuizMode.NAME_POKE) {
                                viewModel.submitLocation(geoPoint)
                            }
                        }
                        tag = controller // store controller in tag to access later if needed
                    }
                },
                update = { mapView ->
                    val controller = mapView.tag as? MapController
                    
                    when (val state = uiState) {
                        is QuizState.Question -> {
                            controller?.clearMarkers()
                            if (state.mode == QuizMode.SHOW_NAME) {
                                if (state.item.type == QuizType.POLYGON) {
                                    val points = GeometryUtils.parseGeometry(state.item.geometryData)
                                    if (points.isNotEmpty()) {
                                        controller?.drawPolygon(points)
                                    } else {
                                        // Fallback to circle if no geometryData
                                        controller?.drawCircle(
                                            GeoPoint(state.item.latitude, state.item.longitude),
                                            state.item.baseRadius.toDouble()
                                        )
                                    }
                                }
                                controller?.addMarker(
                                    state.item.latitude,
                                    state.item.longitude,
                                    "Where is this?"
                                )
                                controller?.centerMap(state.item.latitude, state.item.longitude, state.initialZoom.toDouble())
                            }
                        }
                        is QuizState.Result -> {
                            controller?.clearMarkers()
                            if (state.item.type == QuizType.POLYGON) {
                                val points = GeometryUtils.parseGeometry(state.item.geometryData)
                                if (points.isNotEmpty()) {
                                    controller?.drawPolygon(points)
                                } else {
                                    controller?.drawCircle(
                                        GeoPoint(state.item.latitude, state.item.longitude),
                                        state.item.baseRadius.toDouble()
                                    )
                                }
                            }
                            controller?.addMarker(state.item.latitude, state.item.longitude, state.item.name)
                            state.userLocation?.let {
                                controller?.addMarker(it.latitude, it.longitude, "Your Guess")
                                controller?.drawLine(
                                    org.osmdroid.util.GeoPoint(state.item.latitude, state.item.longitude),
                                    it
                                )
                            }
                        }
                        else -> {}
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Overlay UI
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            when (val state = uiState) {
                is QuizState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                is QuizState.Question -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (state.mode == QuizMode.SHOW_NAME || state.mode == QuizMode.IMAGE_GUESS) {
                                val title = if (state.mode == QuizMode.IMAGE_GUESS) "What is this place?" else "What is highlighted location?"
                                Text(title, style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                state.options.forEach { option ->
                                    Button(
                                        onClick = { viewModel.submitAnswer(option) },
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    ) {
                                        Text(option)
                                    }
                                }
                            } else {
                                Text("Find this place on map:", style = MaterialTheme.typography.titleMedium)
                                Text(state.item.name, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                                Text("(Long press on map to set marker)", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                is QuizState.Result -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(state.message, style = MaterialTheme.typography.titleMedium)
                            if (state.distance > 0) {
                                Text("Distance: ${state.distance.toInt()}m")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.loadNextQuestion() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Next Question")
                            }
                        }
                    }
                }
                QuizState.Empty -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Box(modifier = Modifier.padding(16.dp)) {
                            Text("No quiz items available")
                        }
                    }
                }
            }
        }
    }
        // Back button or Exit logic (simplified as overlay here)
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.TopStart) {
             Button(onClick = { viewModel.backToMenu() }) {
                 Text("Back to Menu")
             }
        }
    }
}

