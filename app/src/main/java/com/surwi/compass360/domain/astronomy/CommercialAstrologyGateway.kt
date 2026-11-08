package com.surwi.compass360.domain.astronomy

import com.surwi.compass360.BuildConfig
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Authenticated client for the first-party commercial calculation gateway.
 * The vendor API key must remain in server-side secret storage, never in the APK.
 */
class CommercialAstrologyGateway(
    private val baseUrl: String = BuildConfig.COMMERCIAL_API_BASE_URL,
    private val executor: Executor = Executors.newFixedThreadPool(2)
) {
    data class BirthInput(
        val date: String,
        val time: String,
        val latitude: Double,
        val longitude: Double,
        val timezoneHours: Double,
        val place: String? = null,
        val engine: String = "drik",
        val language: String = "both"
    )

    sealed class Result {
        data class Success(val json: JSONObject) : Result()
        data class Failure(val message: String, val httpCode: Int? = null) : Result()
    }

    fun panchanga(
        date: String, place: String?, latitude: Double, longitude: Double,
        timezoneHours: Double, bearerToken: String, callback: (Result) -> Unit
    ) = post("/api/panchanga", JSONObject().apply {
        put("date", date); putOptional("place", place)
        put("lat", latitude); put("lon", longitude); put("tz", timezoneHours)
        put("engine", "drik"); put("lang", "both")
    }, bearerToken, callback)

    fun kundali(input: BirthInput, bearerToken: String, callback: (Result) -> Unit) =
        post("/api/kundali", JSONObject().apply {
            put("date", input.date); put("time", input.time)
            put("lat", input.latitude); put("lon", input.longitude)
            put("tz", input.timezoneHours); putOptional("place", input.place)
            put("engine", input.engine); put("lang", input.language)
        }, bearerToken, callback)

    fun muhurta(
        date: String, karya: String, place: String?, latitude: Double, longitude: Double,
        timezoneHours: Double, bearerToken: String, callback: (Result) -> Unit
    ) = post("/api/muhurta", JSONObject().apply {
        put("date", date); put("karya", karya); putOptional("place", place)
        put("lat", latitude); put("lon", longitude); put("tz", timezoneHours)
        put("engine", "drik"); put("lang", "both")
    }, bearerToken, callback)

    private fun JSONObject.putOptional(key: String, value: String?) {
        if (!value.isNullOrBlank()) put(key, value)
    }

    private fun post(path: String, body: JSONObject, token: String, callback: (Result) -> Unit) {
        if (baseUrl.isBlank()) {
            callback(Result.Failure("Commercial gateway URL is not configured."))
            return
        }
        if (token.isBlank()) {
            callback(Result.Failure("Sign in is required before requesting a private calculation."))
            return
        }
        executor.execute {
            var connection: HttpURLConnection? = null
            val result = try {
                connection = (URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 12000
                    readTimeout = 25000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Authorization", "Bearer $token")
                }
                connection.outputStream.use { it.write(body.toString().toByteArray(StandardCharsets.UTF_8)) }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val text = stream?.use { input -> BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8)).readText() }.orEmpty()
                val json = if (text.isBlank()) JSONObject() else JSONObject(text)
                if (code in 200..299 && json.optBoolean("success", true)) Result.Success(json)
                else Result.Failure(json.optString("error").ifBlank { "Calculation service returned HTTP $code" }, code)
            } catch (error: Exception) {
                Result.Failure(error.message ?: "Unable to reach the commercial calculation service.")
            } finally {
                connection?.disconnect()
            }
            android.os.Handler(android.os.Looper.getMainLooper()).post { callback(result) }
        }
    }
}
