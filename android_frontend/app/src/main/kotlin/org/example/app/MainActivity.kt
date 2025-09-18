package org.example.app

import android.app.Activity
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.Integer.max
import java.lang.Integer.min
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import kotlin.math.roundToInt

/**
 * PUBLIC_INTERFACE
 * MainActivity is the entry point of the Weather app.
 *
 * This Activity:
 * - Lets the user type a city name and trigger search.
 * - Calls the OpenWeatherMap API (Current Weather endpoint) to fetch:
 *   temperature (Celsius), condition, humidity, and wind speed.
 * - Displays results in a minimalist card with Ocean Professional styling.
 * - Shows a clear error message when the city is not found or on network issues.
 *
 * Inputs:
 * - UI text input from the user for city name.
 *
 * Outputs:
 * - Updates UI elements with either a result card or error message.
 */
class MainActivity : Activity() {

    // Coroutine scope tied to Activity lifecycle
    private val activityJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + activityJob)

    private lateinit var inputCity: EditText
    private lateinit var searchButton: Button
    private lateinit var errorText: TextView
    private lateinit var resultCard: LinearLayout
    private lateinit var tempText: TextView
    private lateinit var conditionText: TextView
    private lateinit var humidityText: TextView
    private lateinit var windText: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Bind views
        inputCity = findViewById(R.id.inputCity)
        searchButton = findViewById(R.id.searchButton)
        errorText = findViewById(R.id.errorText)
        resultCard = findViewById(R.id.resultCard)
        tempText = findViewById(R.id.tempText)
        conditionText = findViewById(R.id.conditionText)
        humidityText = findViewById(R.id.humidityText)
        windText = findViewById(R.id.windText)
        progressBar = findViewById(R.id.progressBar)

        // Apply gradient background per Ocean Professional theme
        applyGradientBackground()

        searchButton.setOnClickListener {
            performSearch()
        }

        inputCity.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancel coroutines to avoid leaks
        uiScope.cancel()
    }

    private fun applyGradientBackground() {
        // From Ocean Professional: gradient from blue-500/10 to gray-50 approximated
        // Start: #E6EEF9 (light blue-ish) -> End: #F9FAFB (gray-50)
        val startColor = 0xFFE6EEF9.toInt()
        val endColor = 0xFFF9FAFB.toInt()
        val gradient = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(startColor, endColor)
        )
        gradient.cornerRadius = 0f
        findViewById<View>(R.id.rootContainer).background = gradient
    }

    private fun performSearch() {
        val city = inputCity.text.toString().trim()
        if (city.isEmpty()) {
            showError(getString(R.string.error_empty_city))
            return
        }
        setLoading(true)
        errorText.visibility = View.GONE
        resultCard.visibility = View.GONE

        uiScope.launch {
            val result = withContext(Dispatchers.IO) { fetchWeather(city) }
            setLoading(false)
            if (result.errorMessage != null) {
                showError(result.errorMessage!!)
            } else if (result.data != null) {
                bindWeather(result.data!!)
            } else {
                showError(getString(R.string.error_unknown))
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        searchButton.isEnabled = !loading
        inputCity.isEnabled = !loading
        searchButton.alpha = if (loading) 0.6f else 1f
    }

    private fun showError(message: String) {
        errorText.text = message
        errorText.visibility = View.VISIBLE
        resultCard.visibility = View.GONE
    }

    private fun bindWeather(model: WeatherModel) {
        // Ocean Professional style: emphasize temperature and condition
        tempText.text = getString(R.string.label_temp_value, model.temperatureC.roundToInt())
        conditionText.text = model.condition
        humidityText.text = getString(R.string.label_humidity_value, model.humidity)
        windText.text = getString(R.string.label_wind_value, model.windSpeed)
        resultCard.visibility = View.VISIBLE
        errorText.visibility = View.GONE
    }

    // PUBLIC_INTERFACE
    /**
     * Fetch weather data from OpenWeatherMap API using city name.
     *
     * This method uses HttpURLConnection to avoid adding external dependencies.
     * Expects the environment variable/API key to be set via Android manifest meta-data:
     * - Request orchestrator: ensure to set meta-data "OWM_API_KEY" in AndroidManifest via manifestPlaceholders or similar.
     * For this template, we read from string resource for simplicity: @string/openweather_api_key.
     *
     * Returns WeatherResult with either data or errorMessage.
     */
    private fun fetchWeather(city: String): WeatherResult {
        val apiKey = getString(R.string.openweather_api_key)
        if (apiKey.isEmpty() || apiKey == "YOUR_API_KEY_HERE") {
            return WeatherResult(errorMessage = getString(R.string.error_missing_api_key))
        }

        val encodedCity = URLEncoder.encode(city, "UTF-8")
        val urlStr =
            "https://api.openweathermap.org/data/2.5/weather?q=$encodedCity&appid=$apiKey&units=metric"

        var connection: HttpURLConnection? = null
        return try {
            val url = URL(urlStr)
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 10000
                requestMethod = "GET"
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val reader = BufferedReader(InputStreamReader(stream))
            val body = buildString {
                var line: String? = reader.readLine()
                while (line != null) {
                    append(line)
                    line = reader.readLine()
                }
            }
            reader.close()

            if (code == 404) {
                // City not found
                WeatherResult(errorMessage = getString(R.string.error_city_not_found))
            } else if (code !in 200..299) {
                WeatherResult(errorMessage = getString(R.string.error_network_generic))
            } else {
                parseWeather(body)
            }
        } catch (e: Exception) {
            WeatherResult(errorMessage = getString(R.string.error_network_generic))
        } finally {
            connection?.disconnect()
        }
    }

    private fun parseWeather(json: String): WeatherResult {
        return try {
            val root = JSONObject(json)

            val main = root.getJSONObject("main")
            val wind = root.getJSONObject("wind")
            val weatherArray = root.getJSONArray("weather")
            val weather0 = if (weatherArray.length() > 0) weatherArray.getJSONObject(0) else null

            val temp = main.optDouble("temp", Double.NaN)
            val humidity = main.optInt("humidity", -1)
            val windSpeed = wind.optDouble("speed", Double.NaN)
            val condition = weather0?.optString("main", "—") ?: "—"

            if (temp.isNaN() || humidity < 0 || windSpeed.isNaN()) {
                WeatherResult(errorMessage = getString(R.string.error_parse))
            } else {
                WeatherResult(
                    data = WeatherModel(
                        temperatureC = temp,
                        condition = condition,
                        humidity = humidity,
                        windSpeed = windSpeed
                    )
                )
            }
        } catch (e: Exception) {
            WeatherResult(errorMessage = getString(R.string.error_parse))
        }
    }
}

/**
 * WeatherModel holds the parsed weather information displayed to the user.
 */
data class WeatherModel(
    val temperatureC: Double,
    val condition: String,
    val humidity: Int,
    val windSpeed: Double
)

/**
 * WeatherResult wraps either a successful WeatherModel or an error message.
 */
data class WeatherResult(
    val data: WeatherModel? = null,
    val errorMessage: String? = null
)
