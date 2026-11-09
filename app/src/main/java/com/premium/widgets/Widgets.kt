package com.premium.widgets
import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews

fun pushWidget(c: Context, id: Int, type: String) {
    val rv = RemoteViews(c.packageName, R.layout.widget)
    rv.setImageViewBitmap(R.id.img, Renderer.render(c, type, id))
    val i = Intent(c, ConfigActivity::class.java).setAction(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id).setData(Uri.parse("w://$id"))
    rv.setOnClickPendingIntent(R.id.img, PendingIntent.getActivity(c, id, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
    AppWidgetManager.getInstance(c).updateAppWidget(id, rv)
}

abstract class BaseWidget(private val type: String) : AppWidgetProvider() {
    override fun onUpdate(c: Context, m: AppWidgetManager, ids: IntArray) {
        val pr = goAsync()
        Thread {
            try { Data.refresh(c, type); ids.forEach { pushWidget(c, it, type) }; if (type == "clock") tick(c) }
            finally { pr.finish() }
        }.start()
    }
    private fun tick(c: Context) {
        val ids = AppWidgetManager.getInstance(c).getAppWidgetIds(ComponentName(c, javaClass))
        val i = Intent(c, javaClass).setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        val pi = PendingIntent.getBroadcast(c, 0, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val next = (System.currentTimeMillis() / 60000 + 1) * 60000
        (c.getSystemService(Context.ALARM_SERVICE) as AlarmManager).setAndAllowWhileIdle(AlarmManager.RTC, next, pi)
    }
}
class ClockWidget : BaseWidget("clock")
class DateWidget : BaseWidget("date")
class QuoteWidget : BaseWidget("quote")
class WeatherWidget : BaseWidget("weather")
class BatteryWidget : BaseWidget("battery")
class PrayerWidget : BaseWidget("prayer")
