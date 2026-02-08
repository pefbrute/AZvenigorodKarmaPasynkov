package com.example.azvenigorodkarmapasynkov

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.azvenigorodkarmapasynkov.data.DataSeeder
import com.example.azvenigorodkarmapasynkov.data.QuizDatabase
import com.example.azvenigorodkarmapasynkov.data.SRSRepository
import com.example.azvenigorodkarmapasynkov.ui.MapQuizScreen
import com.example.azvenigorodkarmapasynkov.ui.MapQuizViewModel
import com.example.azvenigorodkarmapasynkov.ui.MapQuizViewModelFactory
import com.example.azvenigorodkarmapasynkov.ui.theme.AZvenigorodKarmaPasynkovTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val TAG = "AZvenigorodLog"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "MainActivity created successfully!")

        
        // Manual dependency injection for MVP
        val database = QuizDatabase.getDatabase(this)
        val repository = SRSRepository(database.quizDao())
        val factory = MapQuizViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, factory)[MapQuizViewModel::class.java]

        // Seed data (checks for new items inside)
        lifecycleScope.launch {
            DataSeeder.populateDatabase(this@MainActivity, repository)
        }

        setContent {
            AZvenigorodKarmaPasynkovTheme {
                MapQuizScreen(viewModel = viewModel)
            }
        }
    }
}
