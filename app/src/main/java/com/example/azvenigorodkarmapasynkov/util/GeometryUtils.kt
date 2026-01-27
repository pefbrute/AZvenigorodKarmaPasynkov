package com.example.azvenigorodkarmapasynkov.util

import com.example.azvenigorodkarmapasynkov.data.QuizItem
import com.example.azvenigorodkarmapasynkov.data.QuizType
import org.osmdroid.util.GeoPoint
import kotlin.math.*
import kotlinx.serialization.json.Json

object GeometryUtils {

    // Earth radius in meters
    private const val R = 6371000.0

    fun parseGeometry(json: String): List<GeoPoint> {
        if (json.isBlank()) return emptyList()
        try {
            // Simple parsing: expects [[lat,lon], [lat,lon]]
            // Using basic string manipulation to avoid complex serialization setup if not strictly needed
            // But since we use kotlinx.serialization in project, let's try to use it if possible, 
            // OR just robust regex parsing for this specific format to be safe and dependency-light in util.
            val cleanJson = json.trim().removePrefix("[").removeSuffix("]")
            if (cleanJson.isBlank()) return emptyList()

            val points = mutableListOf<GeoPoint>()
            // Split by "],[" or similar pattern
            var current = cleanJson
            while (current.contains("[")) {
                val start = current.indexOf("[")
                val end = current.indexOf("]")
                if (start != -1 && end != -1) {
                    val pair = current.substring(start + 1, end).split(",")
                    if (pair.size >= 2) {
                        points.add(GeoPoint(pair[0].trim().toDouble(), pair[1].trim().toDouble()))
                    }
                    current = current.substring(end + 1)
                } else {
                    break
                }
            }
            return points
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }

    fun calculateDistanceToItem(userPoint: GeoPoint, item: QuizItem): Double {
        return when (item.type) {
            QuizType.POINT -> {
                 // Distance to center
                 haversine(userPoint.latitude, userPoint.longitude, item.latitude, item.longitude)
            }
            QuizType.LINE -> {
                val points = parseGeometry(item.geometryData)
                if (points.isEmpty()) {
                    haversine(userPoint.latitude, userPoint.longitude, item.latitude, item.longitude)
                } else {
                    distanceToPolyline(userPoint, points)
                }
            }
            QuizType.POLYGON -> {
                val points = parseGeometry(item.geometryData)
                if (points.isEmpty()) {
                    haversine(userPoint.latitude, userPoint.longitude, item.latitude, item.longitude)
                } else {
                    if (isPointInPolygon(userPoint, points)) {
                        0.0
                    } else {
                        distanceToPolygon(userPoint, points)
                    }
                }
            }
        }
    }

    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaPhi = Math.toRadians(lat2 - lat1)
        val deltaLambda = Math.toRadians(lon2 - lon1)

        val a = sin(deltaPhi / 2).pow(2) + cos(phi1) * cos(phi2) * sin(deltaLambda / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return R * c
    }

    private fun distanceToPolyline(p: GeoPoint, poly: List<GeoPoint>): Double {
        var minDst = Double.MAX_VALUE
        for (i in 0 until poly.size - 1) {
            val d = distanceToSegment(p, poly[i], poly[i+1])
            if (d < minDst) minDst = d
        }
        return minDst
    }

    // Distance from point p to segment [a, b]
    private fun distanceToSegment(p: GeoPoint, a: GeoPoint, b: GeoPoint): Double {
        val lat1 = Math.toRadians(a.latitude)
        val lon1 = Math.toRadians(a.longitude)
        val lat2 = Math.toRadians(b.latitude)
        val lon2 = Math.toRadians(b.longitude)
        val lat3 = Math.toRadians(p.latitude)
        val lon3 = Math.toRadians(p.longitude)

        // Equirectangular approximation for small distances (performance)
        // Or project to cartesian? For < 1km, approximation is fine.
        // Let's use simple geometric projection on lat/lon space scaled by cos(lat)
        
        val x = (lon3 - lon1) * cos((lat1 + lat3) / 2)
        val y = lat3 - lat1
        val dx = (lon2 - lon1) * cos((lat1 + lat2) / 2)
        val dy = lat2 - lat1

        if (dx == 0.0 && dy == 0.0) return haversine(p.latitude, p.longitude, a.latitude, a.longitude)

        val t = ((x * dx + y * dy) / (dx * dx + dy * dy)).coerceIn(0.0, 1.0)
        
        val closestLat = lat1 + t * dy
        val closestLon = lon1 + t * (dx / cos((lat1 + closestLat)/2)) // approximate reverse

        // Back to degrees to use haversine for final accurate distance
        return haversine(p.latitude, p.longitude, Math.toDegrees(closestLat), Math.toDegrees(closestLon))
    }
    
    // Ray casting algorithm
    private fun isPointInPolygon(p: GeoPoint, poly: List<GeoPoint>): Boolean {
        var inside = false
        var j = poly.size - 1
        for (i in poly.indices) {
            if ((poly[i].latitude > p.latitude) != (poly[j].latitude > p.latitude) &&
                (p.longitude < (poly[j].longitude - poly[i].longitude) * (p.latitude - poly[i].latitude) / (poly[j].latitude - poly[i].latitude) + poly[i].longitude)
            ) {
                inside = !inside
            }
            j = i
        }
        return inside
    }

    private fun distanceToPolygon(p: GeoPoint, poly: List<GeoPoint>): Double {
         // Distance to closest edge (treating it as a closed polyline)
         val closedPoly = poly + poly[0]
         return distanceToPolyline(p, closedPoly)
    }
}
