package com.example.azvenigorodkarmapasynkov.map

import android.content.Context
import android.preference.PreferenceManager
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.modules.OfflineTileProvider
import org.osmdroid.tileprovider.tilesource.FileBasedTileSource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import android.graphics.Color
import java.io.File

class MapController(private val context: Context, private val mapView: MapView) {

    init {
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
        mapView.setMultiTouchControls(true)
        mapView.setBuiltInZoomControls(false)
    }

    fun setOfflineMap(file: File) {
        if (!file.exists()) return

        // Basic implementation for MBTiles or GEMF
        // For .map (Mapsforge), we need a different library or wrapper, but user mentioned osmdroid supports offline archives.
        // osmdroid supports .sqlite, .mbtiles, .zip, .gemf natively via OfflineTileProvider
        
        try {
            val offlineProvider = OfflineTileProvider(SimpleRegisterReceiver(context), arrayOf(file))
            val archives = offlineProvider.archives
            if (archives.isNotEmpty()) {
                val archive = archives[0]
                val sourceNames = archive.tileSources
                if (sourceNames.isNotEmpty()) {
                    val sourceName = sourceNames.iterator().next()
                    val tileSource = FileBasedTileSource.getSource(sourceName)
                    mapView.setTileSource(tileSource)
                    mapView.setUseDataConnection(false)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun setOnlineMap() {
         mapView.setTileSource(TileSourceFactory.MAPNIK)
    }

    fun centerMap(latitude: Double, longitude: Double, zoom: Double = 15.0) {
        val point = GeoPoint(latitude, longitude)
        mapView.controller.setZoom(zoom)
        mapView.controller.setCenter(point)
    }

    fun addMarker(latitude: Double, longitude: Double, title: String) {
        val marker = Marker(mapView)
        marker.position = GeoPoint(latitude, longitude)
        marker.title = title
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        mapView.overlays.add(marker)
        mapView.invalidate()
    }

    fun clearMarkers() {
        mapView.overlays.removeAll { it is Marker || it is Polyline || it is Polygon }
        mapView.invalidate()
    }

    fun setOnMapClickListener(listener: (GeoPoint) -> Unit) {
        val receiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                return false
            }

            override fun longPressHelper(p: GeoPoint?): Boolean {
                p?.let { listener(it) }
                return true
            }
        }
        val overlay = MapEventsOverlay(receiver)
        mapView.overlays.add(overlay)
    }
    
    fun drawLine(from: GeoPoint, to: GeoPoint) {
        val line = Polyline()
        line.addPoint(from)
        line.addPoint(to)
        mapView.overlays.add(line)
        mapView.invalidate()
    }

    fun drawPolyline(points: List<GeoPoint>, color: Int = Color.BLUE) {
        if (points.isEmpty()) return
        val polyline = Polyline()
        polyline.setPoints(points)
        polyline.outlinePaint.color = color
        polyline.outlinePaint.strokeWidth = 10f
        mapView.overlays.add(polyline)
        mapView.invalidate()
    }

    fun drawPolygon(points: List<GeoPoint>, color: Int = Color.argb(80, 0, 0, 255)) {
        if (points.isEmpty()) return
        val polygon = Polygon()
        polygon.points = points
        polygon.fillPaint.color = color
        polygon.outlinePaint.color = Color.BLUE
        polygon.outlinePaint.strokeWidth = 2f
        mapView.overlays.add(polygon)
        mapView.invalidate()
    }

    fun drawCircle(center: GeoPoint, radiusMeters: Double, color: Int = Color.argb(80, 0, 0, 255)) {
        val circlePoints = Polygon.pointsAsCircle(center, radiusMeters)
        drawPolygon(circlePoints, color)
    }
}
