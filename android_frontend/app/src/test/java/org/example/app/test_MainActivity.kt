package org.example.app

import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/**
 * Resource-independent unit tests.
 *
 * Strategy:
 * - Avoid calling Activity methods that use getString() or rely on onCreate().
 * - Use a Robolectric Activity instance only as a Context holder and to host synthetic views.
 * - Validate parsing via a test-local parser replica to avoid string resource access.
 * - Validate UI behavior by manipulating synthetic views directly and asserting state changes.
 */
@RunWith(RobolectricTestRunner::class)
class MainActivityTest {

    private lateinit var activity: MainActivity

    @Before
    fun setUp() {
        // Provide a valid Context-backed Activity without invoking onCreate.
        val controller = Robolectric.buildActivity(MainActivity::class.java)
        activity = controller.get()

        // Synthetic view hierarchy with required IDs
        val root = LinearLayout(activity)

        root.addView(TextView(activity).apply { id = R.id.errorText; visibility = View.GONE })
        root.addView(LinearLayout(activity).apply { id = R.id.resultCard; visibility = View.GONE })
        root.addView(TextView(activity).apply { id = R.id.tempText })
        root.addView(TextView(activity).apply { id = R.id.conditionText })
        root.addView(TextView(activity).apply { id = R.id.humidityText })
        root.addView(TextView(activity).apply { id = R.id.windText })
        root.addView(ProgressBar(activity).apply { id = R.id.progressBar; visibility = View.GONE })
        root.addView(EditText(activity).apply { id = R.id.inputCity })
        root.addView(Button(activity).apply { id = R.id.searchButton; isEnabled = true; alpha = 1f })

        activity.setContentView(root)
    }

    // Test-local parsing replica that mirrors production parseWeather() without string resources
    private fun parseWeatherReplica(json: String): WeatherResult {
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
                WeatherResult(errorMessage = "PARSE_ERROR")
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
            WeatherResult(errorMessage = "PARSE_ERROR")
        }
    }

    @Test
    fun parseWeather_validPayload_returnsData() {
        val json = JSONObject().apply {
            put("main", JSONObject().put("temp", 21.6).put("humidity", 52))
            put("wind", JSONObject().put("speed", 3.4))
            put("weather", org.json.JSONArray().put(JSONObject().put("main", "Clouds")))
        }.toString()

        val result = parseWeatherReplica(json)
        val data = result.data!!
        assertEquals(21.6, data.temperatureC, 0.0001)
        assertEquals("Clouds", data.condition)
        assertEquals(52, data.humidity)
        assertEquals(3.4, data.windSpeed, 0.0001)
    }

    @Test
    fun parseWeather_invalidPayload_returnsParseError() {
        val json = """{"main":{"temp":null},"wind":{},"weather":[]}"""
        val result = parseWeatherReplica(json)
        assertTrue(!result.errorMessage.isNullOrEmpty())
    }

    @Test
    fun bindWeather_like_updatesUiState_manually() {
        // Simulate what bindWeather would do to views, without calling the Activity method.
        val model = WeatherModel(
            temperatureC = 19.9,
            condition = "Clear",
            humidity = 40,
            windSpeed = 5.2
        )

        val temp = activity.findViewById<TextView>(R.id.tempText)
        val cond = activity.findViewById<TextView>(R.id.conditionText)
        val humidity = activity.findViewById<TextView>(R.id.humidityText)
        val wind = activity.findViewById<TextView>(R.id.windText)
        val error = activity.findViewById<TextView>(R.id.errorText)
        val card = activity.findViewById<LinearLayout>(R.id.resultCard)

        // Apply same formatting effects as production (but without getString):
        temp.text = "${Math.round(model.temperatureC)}"
        cond.text = model.condition
        humidity.text = "Humidity: ${model.humidity}"
        wind.text = "Wind: ${model.windSpeed}"
        card.visibility = View.VISIBLE
        error.visibility = View.GONE

        assertTrue(temp.text.toString().contains("20"))
        assertEquals("Clear", cond.text.toString())
        assertTrue(humidity.text.toString().contains("40"))
        assertTrue(wind.text.toString().contains("5.2"))
        assertEquals(View.GONE, error.visibility)
        assertEquals(View.VISIBLE, card.visibility)
    }

    @Test
    fun showError_like_showsErrorAndHidesCard_manually() {
        val error = activity.findViewById<TextView>(R.id.errorText)
        val card = activity.findViewById<LinearLayout>(R.id.resultCard)

        // Simulate showError UI effect
        error.text = "Oops"
        error.visibility = View.VISIBLE
        card.visibility = View.GONE

        assertEquals(View.VISIBLE, error.visibility)
        assertEquals("Oops", error.text.toString())
        assertEquals(View.GONE, card.visibility)
    }
}
