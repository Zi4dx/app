package com.premium.widgets
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
import android.os.BatteryManager
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.time.*
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object Renderer {
    val themes = listOf(
        intArrayOf(0xFF141E30.toInt(), 0xFF243B55.toInt()), intArrayOf(0xFF000000.toInt(), 0xFF1C1C1E.toInt()),
        intArrayOf(0xFF0F2027.toInt(), 0xFF2C5364.toInt()), intArrayOf(0xFF2B1055.toInt(), 0xFF7597DE.toInt()))
    val accents = intArrayOf(0xFFFFD700.toInt(), 0xFF4FC3F7.toInt(), 0xFF69F0AE.toInt(), 0xFFFF8A80.toInt(), 0xFFFFFFFF.toInt())
    private val quotes = listOf("من جدّ وجد، ومن زرع حصد.", "العلم نور، والجهل ظلام.", "الوقت كالسيف إن لم تقطعه قطعك.",
        "لا تؤجل عمل اليوم إلى الغد.", "خير الكلام ما قلّ ودلّ.", "الصبر مفتاح الفرج.", "اطلبوا العلم من المهد إلى اللحد.")
    private val wx = mapOf(0 to "صافٍ", 1 to "صافٍ غالباً", 2 to "غائم جزئياً", 3 to "غائم", 45 to "ضباب", 48 to "ضباب",
        51 to "رذاذ", 53 to "رذاذ", 55 to "رذاذ", 61 to "مطر خفيف", 63 to "مطر", 65 to "مطر غزير", 80 to "زخات مطر", 95 to "عاصفة رعدية")

    fun render(c: Context, type: String, id: Int): Bitmap {
        val g = Cfg.get(c, id); val W = 640; val H = 320
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888); val cv = Canvas(bmp)
        val th = themes[g.theme.coerceIn(0, themes.lastIndex)]; val ac = accents[g.accent.coerceIn(0, accents.lastIndex)]
        val r = RectF(3f, 3f, W - 3f, H - 3f); val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = LinearGradient(0f, 0f, W.toFloat(), H.toFloat(), th[0], th[1], Shader.TileMode.CLAMP)
        cv.drawRoundRect(r, 56f, 56f, p)
        p.shader = null; p.style = Paint.Style.STROKE; p.strokeWidth = 3f; p.color = 0x33FFFFFF
        cv.drawRoundRect(r, 56f, 56f, p)
        val tp = TextPaint(Paint.ANTI_ALIAS_FLAG); tp.typeface = Fonts.load(c, g.font); tp.textAlign = Paint.Align.CENTER
        fun t(s: String, y: Float, size: Float, col: Int) { tp.textSize = size; tp.color = col; cv.drawText(s, W / 2f, y, tp) }
        val white = 0xFFFFFFFF.toInt(); val dim = 0xB3FFFFFF.toInt(); val loc = Locale.getDefault()
        val sp = Data.sp(c)
        when (type) {
            "clock" -> { val n = LocalDateTime.now()
                t(n.format(DateTimeFormatter.ofPattern("HH:mm")), 200f, 140f, ac)
                t(n.format(DateTimeFormatter.ofPattern("EEEE d MMMM", loc)), 268f, 38f, white) }
            "date" -> { val d = LocalDate.now()
                t(d.dayOfMonth.toString(), 170f, 140f, ac)
                t(d.format(DateTimeFormatter.ofPattern("EEEE MMMM yyyy", loc)), 235f, 36f, white)
                try { t(HijrahDate.from(d).format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ar"))), 285f, 32f, dim) } catch (_: Exception) { } }
            "quote" -> { val q = quotes[LocalDate.now().dayOfYear % quotes.size]
                tp.textAlign = Paint.Align.LEFT; tp.textSize = 46f; tp.color = white
                val sl = StaticLayout.Builder.obtain(q, 0, q.length, tp, W - 120).setAlignment(Layout.Alignment.ALIGN_CENTER).build()
                cv.save(); cv.translate(60f, (H - sl.height) / 2f); sl.draw(cv); cv.restore()
                tp.textAlign = Paint.Align.CENTER; t("❝", 70f, 60f, ac) }
            "weather" -> { val has = sp.contains("wt")
                t(if (has) "${sp.getInt("wt", 0)}°" else "—", 195f, 150f, ac)
                t(if (has) wx[sp.getInt("wc", 0)] ?: "—" else "جارٍ التحديث", 265f, 40f, white) }
            "battery" -> { val i = c.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                val pct = (i?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0) * 100 / (i?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100)
                val chg = (i?.getIntExtra(BatteryManager.EXTRA_STATUS, 0) ?: 0) == BatteryManager.BATTERY_STATUS_CHARGING
                t("$pct%", 170f, 130f, ac)
                p.style = Paint.Style.FILL; p.color = 0x33FFFFFF; cv.drawRoundRect(RectF(80f, 210f, 560f, 236f), 13f, 13f, p)
                p.color = ac; cv.drawRoundRect(RectF(80f, 210f, 80f + 480f * pct / 100f, 236f), 13f, 13f, p)
                t(if (chg) "قيد الشحن" else "البطارية", 290f, 34f, dim) }
            "prayer" -> { val ns = listOf("Fajr" to "الفجر", "Dhuhr" to "الظهر", "Asr" to "العصر", "Maghrib" to "المغرب", "Isha" to "العشاء")
                if (!sp.contains("p_Fajr")) t("جارٍ التحديث", 190f, 60f, ac) else {
                    val now = LocalTime.now(); var pick = ns[0]; var tm = LocalTime.parse(sp.getString("p_Fajr", "05:00")); var next = true
                    for (n in ns) { val x = LocalTime.parse(sp.getString("p_${n.first}", "00:00")); if (x.isAfter(now)) { pick = n; tm = x; next = false; break } }
                    var m = Duration.between(now, tm).toMinutes(); if (next) m += 1440
                    t(pick.second, 150f, 80f, ac); t(tm.toString(), 235f, 70f, white)
                    t("بعد ${m / 60} س ${m % 60} د", 288f, 32f, dim) } }
        }
        return bmp
    }
}
