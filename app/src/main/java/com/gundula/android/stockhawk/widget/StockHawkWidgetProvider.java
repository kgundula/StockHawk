package com.gundula.android.stockhawk.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.RemoteViews;

import com.gundula.android.stockhawk.R;
import com.gundula.android.stockhawk.ui.StockDetailsActivity;

/**
 * Created by kgundula on 2016/10/14.
 */

public class StockHawkWidgetProvider extends AppWidgetProvider {

    public static final String INTENT_ACTION = "INTENT_ACTION";
    public static final String EXTRA_SYMBOL = "EXTRA_SYMBOL";
    public static final String EXTRA_BID_PRICE = "EXTRA_BID_PRICE";
    public static final String EXTRA_PERCENT_CHANGE = "EXTRA_PERCENT_CHANGE";
    public static final String EXTRA_CHANGE = "EXTRA_CHANGE";
    public static final String EXTRA_IS_UP = "EXTRA_IS_UP";


    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (INTENT_ACTION.equals(intent.getAction())) {

            Intent stockDetailsIntent = new Intent(context, StockDetailsActivity.class);
            stockDetailsIntent.putExtra("symbol", intent.getStringExtra(EXTRA_SYMBOL));
            stockDetailsIntent.putExtra("bid_price", intent.getStringExtra(EXTRA_BID_PRICE));
            stockDetailsIntent.putExtra("percent_change", intent.getStringExtra(EXTRA_PERCENT_CHANGE));
            stockDetailsIntent.putExtra("change", intent.getStringExtra(EXTRA_CHANGE));
            stockDetailsIntent.putExtra("is_up", intent.getStringExtra(EXTRA_IS_UP));
            stockDetailsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(stockDetailsIntent);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int i = 0; i < appWidgetIds.length; i++) {
            Intent intent = new Intent(context, StockHawkWidgetService.class);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));

            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_stock);
            remoteViews.setRemoteAdapter(appWidgetIds[i], R.id.stock_widget_layout, intent);
            remoteViews.setEmptyView(R.id.stock_widget_layout, R.id.empty_stocks_widget_layout);

            Intent stockDetails = new Intent(context, StockHawkWidgetProvider.class);
            stockDetails.setAction(StockHawkWidgetProvider.INTENT_ACTION);
            stockDetails.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, stockDetails, PendingIntent.FLAG_UPDATE_CURRENT);

            remoteViews.setPendingIntentTemplate(R.id.stock_widget_layout, pendingIntent);
            appWidgetManager.updateAppWidget(appWidgetIds[i], remoteViews);

        }

        super.onUpdate(context, appWidgetManager, appWidgetIds);
        context.startService(new Intent(context, StockHawkWidgetService.class));
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {

        context.startService(new Intent(context, StockHawkWidgetService.class));
    }

}
