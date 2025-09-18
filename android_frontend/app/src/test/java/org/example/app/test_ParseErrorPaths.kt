package org.example.app

import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.robolectric.Robolectric
import org.robolectric.annotation.Config

/**
 * Additional parse edge cases for MainActivity.
 */
@Config(sdk = [34])
class ParseErrorPathsTest {

    @Test
    fun parse_missingFields_returnsParseError() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).create().start()
        val activity = controller.get()

        val json = JSONObject().apply {
            put("main", JSONObject()) // missing temp/humidity
            put("wind", JSONObject()) // missing speed
            put("weather", org.json.JSONArray())
        }.toString()

        val method = MainActivity::class.java.getDeclaredMethod("parseWeather", String::class.java)
        method.isAccessible = true
        val result = method.invoke(activity, json) as WeatherResult
        assertEquals(activity.getString(R.string.error_parse), result.errorMessage)
    }
}
