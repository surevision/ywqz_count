package com.soulbrz.ywqz;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

public class CountWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_CLICK = "com.soulbrz.ywqz.action.WIDGET_CLICK";
    public static final String ACTION_REFRESH = "com.soulbrz.ywqz.action.WIDGET_REFRESH";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (ACTION_CLICK.equals(action)) {
            handleClick(context);
            return;
        }
        if (ACTION_REFRESH.equals(action)) {
            refresh(context);
            return;
        }
        super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        refresh(context);
    }

    private static void handleClick(Context context) {
        if (Api.isInCooldown(context)) {
            refresh(context);
            return;
        }
        Api.markClicked(context);
        scheduleRefresh(context);
        refresh(context);
        CountService.enqueue(context);
    }

    private static void scheduleRefresh(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        Intent intent = new Intent(context, CountWidgetProvider.class);
        intent.setAction(ACTION_REFRESH);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + Api.COOLDOWN_MS,
                pendingIntent);
    }

    public static void refresh(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, CountWidgetProvider.class));
        if (ids == null || ids.length == 0) {
            return;
        }
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_button);
        for (int id : ids) {
            views.setOnClickPendingIntent(R.id.btn_widget, clickIntent(context, id));
            manager.updateAppWidget(id, views);
        }
    }

    private static PendingIntent clickIntent(Context context, int appWidgetId) {
        Intent intent = new Intent(context, CountWidgetProvider.class);
        intent.setAction(ACTION_CLICK);
        intent.setData(Uri.parse("ywqz://widget/click/" + appWidgetId));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        return PendingIntent.getBroadcast(context, appWidgetId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
