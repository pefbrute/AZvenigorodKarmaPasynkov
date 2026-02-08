package com.example.azvenigorodkarmapasynkov.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

import android.util.Log

object DataSeeder {
    private const val TAG = "AZvenigorodSeeder"

    suspend fun populateDatabase(context: Context, repository: SRSRepository) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting population...")
                val jsonString = readAsset(context, "zvenigorod_seed.json")
                if (jsonString.isNotEmpty()) {
                    Log.d(TAG, "JSON read successfully, parsing...")
                    parseAndInsert(jsonString, repository)
                    Log.d(TAG, "Population complete.")
                } else {
                    Log.e(TAG, "Failed to read JSON asset.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error populating database", e)
                e.printStackTrace()
            }
        }
    }

    private fun readAsset(context: Context, fileName: String): String {
        return try {
            val inputStream = context.assets.open(fileName)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val sb = StringBuilder()
            var line: String? = reader.readLine()
            while (line != null) {
                sb.append(line)
                line = reader.readLine()
            }
            reader.close()
            sb.toString()
        } catch (e: Exception) {
            Log.e("AZvenigorodSeeder", "Error reading asset $fileName", e)
            e.printStackTrace()
            ""
        }
    }

    private suspend fun parseAndInsert(json: String, repository: SRSRepository) {
        val root = JSONObject(json)
        val itemsArray = root.getJSONArray("items")
        Log.d("AZvenigorodSeeder", "Found ${itemsArray.length()} items in JSON")
        
        val quizItems = mutableListOf<QuizItem>()
        
        for (i in 0 until itemsArray.length()) {
            val obj = itemsArray.getJSONObject(i)
            
            // "alts" array -> description
            val altsArr = obj.optJSONArray("alts")
            val altsList = mutableListOf<String>()
            if (altsArr != null) {
                for (j in 0 until altsArr.length()) {
                    altsList.add(altsArr.getString(j))
                }
            }
            val kind = obj.optString("kind", "")
            val group = obj.optString("group", "")
            
            val description = "Kind: $kind, Group: $group. " + 
                              (if (altsList.isNotEmpty()) "Alt names: ${altsList.joinToString(", ")}" else "")

            val lat = obj.getDouble("lat")
            val lon = obj.getDouble("lon")
            val answerRadiusM = obj.optInt("answerRadiusM", 150)
            
            // Map kind to QuizType logic:
            // district_anchor, quarter -> potentially POLYGON if I had geometry.
            // But JSON only gives point. So all are POINT for now.
            // We can manually set some to Polygon later.
            
            val imageName = obj.optString("imageName", null)
            var geometry = obj.optString("geometryData", "")
            
            // Handle new "area" object format if geometryData is missing
            if (geometry.isEmpty()) {
                val areaObj = obj.optJSONObject("area")
                if (areaObj != null) {
                    val pointsArr = areaObj.optJSONArray("points")
                    if (pointsArr != null) {
                        geometry = pointsArr.toString()
                    }
                }
            }
            
            val type = when {
                geometry.isNotEmpty() && (geometry.contains("[[") || geometry.contains("[")) -> QuizType.POLYGON
                kind == "district_anchor" || kind == "quarter" -> QuizType.POLYGON
                else -> QuizType.POINT
            }
            
            val item = QuizItem(
                name = obj.getString("name"),
                description = description,
                imageName = imageName,
                latitude = lat,
                longitude = lon,
                type = type,
                baseRadius = answerRadiusM,
                geometryData = geometry
            )
            quizItems.add(item)
        }
        
        repository.addItems(quizItems)
        Log.d("AZvenigorodSeeder", "Inserted ${quizItems.size} items into DB")
    }
}
