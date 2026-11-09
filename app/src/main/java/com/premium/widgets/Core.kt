package com.premium.widgets
import android.content.Context
import android.graphics.Typeface
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

data class Cfg(val font: String = "", val theme: Int = 0, val accent: Int = 0) {
    companion object {
        fun get(c: Context, id: Int) = Data.sp(c).let { Cfg(it.getString("f$id", "")!!, it.getInt("t$id", 0), it.getInt("a$id", 0)) }
        fun save(c: Context, id: Int, g: Cfg) = Data.sp(c).edit().putString("f$id", g.font).putInt("t$id", g.theme).putInt("a$id", g.accent).apply()
    }
}

object Fonts {
    private val cache = HashMap<String, Typeface>()
    fun list(c: Context): List<String> =
        (c.assets.list("fonts")?.filter { it.endsWith(".ttf", true) || it.endsWith(".otf", true) } ?: emptyList()).sorted()
    fun load(c: Context, n: String): Typeface = if (n.isEmpty()) Typeface.DEFAULT else
        cache.getOrPut(n) { try { Typeface.createFromAsset(c.assets, "fonts/$n") } catch (e: Exception) { Typeface.DEFAULT } }
}

object Data {
    fun sp(c: Context) = c.getSharedPreferences("w", 0)
    fun loc(c: Context): Pair<Double, Double> {
        val s = sp(c).getString("loc", "32.8872,13.1913")!!.split(",")
        return (s[0].trim().toDoubleOrNull() ?: 32.8872) to (s.getOrNull(1)?.trim()?.toDoubleOrNull() ?: 13.1913)
    }
    private fun get(url: String): String {
        val u = URL(url).openConnection() as HttpURLConnection
        u.connectTimeout = 8000; u.readTimeout = 8000
        return try { u.inputStream.bufferedReader().readText() } finally { u.disconnect() }
    }
    /** Weather: Open-Meteo (no key). Prayer: Aladhan API (method 3 = MWL, change if needed). */
    fun refresh(c: Context, type: String) {
        try {
            val (la, lo) = loc(c)
            if (type == "weather") {
                val j = JSONObject(get("https://api.open-meteo.com/v1/forecast?latitude=$la&longitude=$lo&current=temperature_2m,weather_code")).getJSONObject("current")
                sp(c).edit().putInt("wt", Math.round(j.getDouble("temperature_2m")).toInt()).putInt("wc", j.getInt("weather_code")).apply()
            }
            if (type == "prayer") {
                val today = LocalDate.now().toString()
                if (sp(c).getString("pd", "") != today) {
                    val t = JSONObject(get("https://api.aladhan.com/v1/timings?latitude=$la&longitude=$lo&method=3")).getJSONObject("data").getJSONObject("timings")
                    val e = sp(c).edit()
                    listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha").forEach { e.putString("p_$it", t.getString(it).take(5)) }
                    e.putString("pd", today).apply()
                }
            }
        } catch (_: Exception) { }
    }
}
