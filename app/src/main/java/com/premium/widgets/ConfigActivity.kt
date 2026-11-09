package com.premium.widgets
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*

/** Simple editor: font library, theme, accent colour, location (weather/prayer). */
class ConfigActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 0)
        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
        if (id == 0) { finish(); return }
        val type = AppWidgetManager.getInstance(this).getAppWidgetInfo(id).provider.className
            .substringAfterLast('.').removeSuffix("Widget").lowercase()
        var cfg = Cfg.get(this, id)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 40, 40, 40) }
        val prev = ImageView(this)
        fun refresh() { Cfg.save(this, id, cfg); prev.setImageBitmap(Renderer.render(this, type, id)) }
        root.addView(prev, LinearLayout.LayoutParams(-1, 520))

        val fonts = listOf("") + Fonts.list(this)
        val sp = Spinner(this)
        sp.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, fonts.map { if (it.isEmpty()) "الخط الافتراضي" else it.substringBeforeLast('.') })
        sp.setSelection(fonts.indexOf(cfg.font).coerceAtLeast(0))
        sp.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, i: Long) { cfg = cfg.copy(font = fonts[pos]); refresh() }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        root.addView(TextView(this).apply { text = "الخط (ضع ملفات ttf في assets/fonts)" }); root.addView(sp)

        fun row(label: String, colors: List<Int>, onPick: (Int) -> Unit) {
            root.addView(TextView(this).apply { text = label; setPadding(0, 24, 0, 0) })
            val r = LinearLayout(this)
            colors.forEachIndexed { i, col -> r.addView(Button(this).apply { setBackgroundColor(col); setOnClickListener { onPick(i); refresh() } }, LinearLayout.LayoutParams(0, 110, 1f).apply { setMargins(6, 6, 6, 6) }) }
            root.addView(r)
        }
        row("الثيم", Renderer.themes.map { it[1] }) { cfg = cfg.copy(theme = it) }
        row("لون التمييز", Renderer.accents.toList()) { cfg = cfg.copy(accent = it) }

        val loc = EditText(this).apply { setText(Data.sp(this@ConfigActivity).getString("loc", "32.8872,13.1913")); hint = "lat,lon" }
        if (type == "weather" || type == "prayer") { root.addView(TextView(this).apply { text = "الموقع (خط العرض,خط الطول)"; setPadding(0, 24, 0, 0) }); root.addView(loc) }

        root.addView(Button(this).apply { text = "حفظ"; setOnClickListener {
            Data.sp(this@ConfigActivity).edit().putString("loc", loc.text.toString()).remove("pd").apply()
            Thread { Data.refresh(this@ConfigActivity, type); pushWidget(this@ConfigActivity, id, type) }.start()
            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)); finish() } })
        setContentView(ScrollView(this).apply { addView(root) })
        refresh()
    }
}
