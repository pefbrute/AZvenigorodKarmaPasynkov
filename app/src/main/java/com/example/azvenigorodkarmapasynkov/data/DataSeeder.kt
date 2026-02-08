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
            
            val imageName = if (obj.isNull("imageName")) null else obj.getString("imageName")
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
                kind == "street" || kind == "road" -> QuizType.LINE
                else -> QuizType.POINT
            }
            
            val imagesArr = obj.optJSONArray("images")
            val baseId = obj.getString("id")
            
            if (imagesArr != null && imagesArr.length() > 0) {
                // Multi-image item: create a QuizItem for each image
                for (j in 0 until imagesArr.length()) {
                    val imgObj = imagesArr.getJSONObject(j)
                    val imgName = imgObj.getString("imageName")
                    val imgLat = imgObj.optDouble("lat", lat)
                    val imgLon = imgObj.optDouble("lon", lon)
                    val itemId = "${baseId}_$j"
                    
                    val existing = repository.getItemById(itemId)
                    if (existing == null) {
                        quizItems.add(QuizItem(
                            id = itemId,
                            name = obj.getString("name"),
                            description = description,
                            imageName = imgName,
                            latitude = imgLat,
                            longitude = imgLon,
                            type = type,
                            baseRadius = answerRadiusM,
                            geometryData = geometry
                        ))
                    } else {
                        // Check if metadata changed
                        if (existing.name != obj.getString("name") || 
                            existing.imageName != imgName || 
                            existing.geometryData != geometry ||
                            existing.latitude != imgLat ||
                            existing.longitude != imgLon) {
                            
                            val updated = existing.copy(
                                name = obj.getString("name"),
                                imageName = imgName,
                                geometryData = geometry,
                                latitude = imgLat,
                                longitude = imgLon,
                                description = description,
                                baseRadius = answerRadiusM,
                                type = type
                            )
                            repository.updateItem(updated)
                        }
                    }
                }
            } else {
                // Standard single-image item
                val existing = repository.getItemById(baseId)
                if (existing == null) {
                    quizItems.add(QuizItem(
                        id = baseId,
                        name = obj.getString("name"),
                        description = description,
                        imageName = imageName,
                        latitude = lat,
                        longitude = lon,
                        type = type,
                        baseRadius = answerRadiusM,
                        geometryData = geometry
                    ))
                } else {
                    if (existing.name != obj.getString("name") || 
                        existing.imageName != imageName || 
                        existing.geometryData != geometry ||
                        existing.latitude != lat ||
                        existing.longitude != lon) {
                        
                        val updated = existing.copy(
                            name = obj.getString("name"),
                            imageName = imageName,
                            geometryData = geometry,
                            latitude = lat,
                            longitude = lon,
                            description = description,
                            baseRadius = answerRadiusM,
                            type = type
                        )
                        repository.updateItem(updated)
                    }
                }
            }
        }
        
        repository.addItems(quizItems)
        Log.d("AZvenigorodSeeder", "Inserted ${quizItems.size} items into DB")
    }
}
