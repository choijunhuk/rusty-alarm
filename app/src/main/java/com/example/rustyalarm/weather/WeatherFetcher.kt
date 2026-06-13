package com.example.rustyalarm.weather

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * Weather snapshot. [tempCelsius] is "now" at the user's location; [feelsLike],
 * [minC]/[maxC] and [precipProb] describe today. [hourlyTempAt] is the forecast
 * temperature for a specific hour today (used to show what the weather will be
 * when an alarm actually fires).
 */
data class Weather(
    val tempCelsius: Double,
    val weatherCode: Int,
    val feelsLike: Double = Double.NaN,
    val minC: Double = Double.NaN,
    val maxC: Double = Double.NaN,
    val precipProb: Int = -1,                  // 0..100, -1 = unknown
    val sourceLat: Double = Double.NaN,
    val sourceLon: Double = Double.NaN,
    private val hourlyTemps: DoubleArray = DoubleArray(0),
    private val hourlyCodes: IntArray = IntArray(0),
    private val firstHourEpoch: Long = 0L,
) {
    val emoji: String = codeToEmoji(weatherCode)

    /** Forecast at the given epoch-millis if it falls in the cached window. */
    fun forecastAt(epochMs: Long): Pair<Double, String>? {
        if (hourlyTemps.isEmpty() || firstHourEpoch == 0L) return null
        val idx = ((epochMs - firstHourEpoch) / (60L * 60_000L)).toInt()
        if (idx !in hourlyTemps.indices) return null
        return hourlyTemps[idx] to codeToEmoji(hourlyCodes.getOrElse(idx) { weatherCode })
    }

    companion object {
        fun codeToEmoji(code: Int): String = when (code) {
            0 -> "☀️"
            1, 2 -> "🌤"
            3 -> "☁️"
            45, 48 -> "🌫"
            in 51..57 -> "🌦"
            in 61..67 -> "🌧"
            in 71..77 -> "🌨"
            in 80..82 -> "🌧"
            in 85..86 -> "🌨"
            in 95..99 -> "⛈"
            else -> "🌡"
        }
    }
}

object WeatherFetcher {

    private const val DEFAULT_LAT = 37.5665   // Seoul fallback
    private const val DEFAULT_LON = 126.9780
    private val CACHE_TTL_MS = TimeUnit.MINUTES.toMillis(30)
    private const val LOCATION_DRIFT_KM = 5.0  // refetch if user moved more than this

    @Volatile private var cached: Weather? = null
    @Volatile private var cachedAt: Long = 0L
    private val fetchMutex = Mutex()

    /**
     * Returns current + forecast weather for the user's location (if permission
     * granted), falling back to coarse last-known location, then IP geolocation,
     * then a Seoul default. Cached for 30 minutes per coordinate.
     */
    suspend fun current(context: Context? = null): Weather? {
        val (lat, lon) = context?.let { resolveLocation(it) } ?: (DEFAULT_LAT to DEFAULT_LON)
        val w = currentAt(lat, lon)
        if (w != null && context != null) writePrecipMirror(context, w.precipProb)
        return w
    }

    /** Mirror today's precip probability so the alarm scheduler can read it
     *  synchronously without doing a network fetch from a BroadcastReceiver. */
    private fun writePrecipMirror(context: Context, precip: Int) {
        context.getSharedPreferences(
            com.example.rustyalarm.prefs.UserPreferences.MIRROR_FILE,
            Context.MODE_PRIVATE,
        ).edit()
            .putInt(PRECIP_MIRROR_KEY, precip)
            .putLong(PRECIP_MIRROR_AT_KEY, System.currentTimeMillis())
            .apply()
    }

    const val PRECIP_MIRROR_KEY = "weather_precip"
    const val PRECIP_MIRROR_AT_KEY = "weather_precip_at"

    suspend fun currentAt(latitude: Double, longitude: Double): Weather? {
        val now = System.currentTimeMillis()
        cached?.let { c ->
            if (now - cachedAt < CACHE_TTL_MS &&
                distanceKm(c.sourceLat, c.sourceLon, latitude, longitude) < LOCATION_DRIFT_KM
            ) return c
        }
        // Serialise concurrent fetches so two callers don't both hit the
        // network when the cache is cold; the second one sees the fresh
        // result after the first releases the mutex.
        return fetchMutex.withLock {
            val now2 = System.currentTimeMillis()
            cached?.let { c ->
                if (now2 - cachedAt < CACHE_TTL_MS &&
                    distanceKm(c.sourceLat, c.sourceLon, latitude, longitude) < LOCATION_DRIFT_KM
                ) return@withLock c
            }
            val fresh = fetch(latitude, longitude)
            if (fresh != null) {
                cached = fresh
                cachedAt = now2
                fresh
            } else cached
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun resolveLocation(context: Context): Pair<Double, Double> {
        // Honour fine/coarse location permission if granted.
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            val client = LocationServices.getFusedLocationProviderClient(context)
            val loc = runCatching {
                withTimeoutOrNull(3_000) {
                    suspendCancellableCoroutine<android.location.Location?> { cont ->
                        client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                            .addOnSuccessListener { cont.resume(it) }
                            .addOnFailureListener { cont.resume(null) }
                    }
                } ?: runCatching { client.lastLocation.await() }.getOrNull()
            }.getOrNull()
            if (loc != null) return loc.latitude to loc.longitude
        }
        // IP-based fallback. Cheap, no key.
        val ip = runCatching {
            withContext(Dispatchers.IO) {
                val url = URL("https://ipapi.co/json/")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 4_000; readTimeout = 4_000
                }
                try {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val obj = JSONObject(body)
                    obj.optDouble("latitude") to obj.optDouble("longitude")
                } finally { runCatching { conn.disconnect() } }
            }
        }.getOrNull()
        if (ip != null && !ip.first.isNaN() && !ip.second.isNaN()) return ip
        return DEFAULT_LAT to DEFAULT_LON
    }

    private suspend fun fetch(
        latitude: Double,
        longitude: Double,
    ): Weather? = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL(
                "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=$latitude&longitude=$longitude" +
                    "&current=temperature_2m,weather_code,apparent_temperature" +
                    "&hourly=temperature_2m,weather_code" +
                    "&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max" +
                    "&forecast_days=2" +
                    "&timezone=auto"
            )
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5_000
                readTimeout = 5_000
            }
            try {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(body)
                val current = root.optJSONObject("current") ?: return@withContext null

                // Daily values index 0 = today
                val daily = root.optJSONObject("daily")
                val maxC = daily?.optJSONArray("temperature_2m_max")?.optDouble(0) ?: Double.NaN
                val minC = daily?.optJSONArray("temperature_2m_min")?.optDouble(0) ?: Double.NaN
                val precip = daily?.optJSONArray("precipitation_probability_max")?.optInt(0, -1) ?: -1

                // Hourly forecast — store raw arrays + epoch-of-first-hour for forecastAt()
                val hourly = root.optJSONObject("hourly")
                val timesJson = hourly?.optJSONArray("time")
                val tempsJson = hourly?.optJSONArray("temperature_2m")
                val codesJson = hourly?.optJSONArray("weather_code")
                val firstEpoch: Long = if (timesJson != null && timesJson.length() > 0) {
                    runCatching { parseIsoLocal(timesJson.optString(0), root.optString("timezone")) }
                        .getOrDefault(0L)
                } else 0L
                val temps = DoubleArray(tempsJson?.length() ?: 0) { i ->
                    tempsJson!!.optDouble(i, Double.NaN)
                }
                val codes = IntArray(codesJson?.length() ?: 0) { i ->
                    codesJson!!.optInt(i, -1)
                }

                Weather(
                    tempCelsius = current.optDouble("temperature_2m", Double.NaN),
                    weatherCode = current.optInt("weather_code", -1),
                    feelsLike   = current.optDouble("apparent_temperature", Double.NaN),
                    minC = minC,
                    maxC = maxC,
                    precipProb = precip,
                    sourceLat = latitude,
                    sourceLon = longitude,
                    hourlyTemps = temps,
                    hourlyCodes = codes,
                    firstHourEpoch = firstEpoch,
                )
            } finally {
                runCatching { conn.disconnect() }
            }
        }.getOrNull()
    }

    /** Parse Open-Meteo's `2026-06-13T07:00` style local time into epoch ms. */
    private fun parseIsoLocal(iso: String, tz: String): Long {
        if (iso.isBlank()) return 0L
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm", java.util.Locale.US)
        fmt.timeZone = java.util.TimeZone.getTimeZone(tz.ifBlank { "UTC" })
        return runCatching { fmt.parse(iso)?.time ?: 0L }.getOrDefault(0L)
    }

    /** Great-circle distance in km between two coords (haversine). */
    private fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        if (lat1.isNaN() || lon1.isNaN()) return Double.MAX_VALUE
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2).let { it * it } +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2).let { it * it }
        return 2 * r * Math.asin(Math.min(1.0, Math.sqrt(a)))
    }
}
