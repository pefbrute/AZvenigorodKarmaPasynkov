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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.azvenigorodkarmapasynkov.map.MapController
import org.osmdroid.views.MapView

@Composable
fun MapQuizScreen(viewModel: MapQuizViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
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
                            if (state.mode == QuizMode.SHOW_NAME) {
                                Text("What is highlighted location?", style = MaterialTheme.typography.titleMedium)
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
}
