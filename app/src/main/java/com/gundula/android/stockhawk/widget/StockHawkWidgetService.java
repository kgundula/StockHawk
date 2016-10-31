package com.gundula.android.stockhawk.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.gundula.android.stockhawk.R;
import com.gundula.android.stockhawk.data.QuoteColumns;
import com.gundula.android.stockhawk.data.QuoteProvider;

import static com.gundula.android.stockhawk.ui.MyStocksActivity.COL_STOCK_BID_PRICE;
import static com.gundula.android.stockhawk.ui.MyStocksActivity.COL_STOCK_CHANGE;
import static com.gundula.android.stockhawk.ui.MyStocksActivity.COL_STOCK_IS_UP;
import static com.gundula.android.stockhawk.ui.MyStocksActivity.COL_STOCK_PERCENT_CHANGE;
import static com.gundula.android.stockhawk.ui.MyStocksActivity.COL_STOCK_SYMBOL;

/**
 * Created by kgundula on 2016/10/16.
 */

public class StockHawkWidgetService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new StockRemoteViewFactory(this.getApplicationContext(), intent);
    }


    public class StockRemoteViewFactory implements RemoteViewsFactory {


        Context context;
        Cursor cursor;
        int appWidgetId;

        StockRemoteViewFactory(Context context, Intent intent) {
            this.context = context;
            this.appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        @Override
        public void onCreate() {
            cursor = getContentResolver().query(
                    QuoteProvider.Quotes.CONTENT_URI,
                    new String[]{QuoteColumns._ID,
                            QuoteColumns.SYMBOL,
                            QuoteColumns.BIDPRICE,
                            QuoteColumns.PERCENT_CHANGE,
                            QuoteColumns.CHANGE,
                            QuoteColumns.ISUP},
                    QuoteColumns.ISCURRENT + " = ?",
                    new String[]{"1"},
                    null);
        }

        @Override
        public void onDataSetChanged() {
            cursor = getContentResolver().query(
                    QuoteProvider.Quotes.CONTENT_URI,
                    new String[]{QuoteColumns._ID,
                            QuoteColumns.SYMBOL,
                            QuoteColumns.BIDPRICE,
                            QuoteColumns.PERCENT_CHANGE,
                            QuoteColumns.CHANGE,
                            QuoteColumns.ISUP},
                    QuoteColumns.ISCURRENT + " = ?",
                    new String[]{"1"},
                    null);
        }

        @Override
        public void onDestroy() {
            if (this.cursor != null)
                this.cursor.close();
        }

        @Override
        public int getCount() {
            return (this.cursor != null) ? this.cursor.getCount() : 0;

        }

        @Override
        public RemoteViews getViewAt(int position) {

            RemoteViews remoteViews = new RemoteViews(this.context.getPackageName(), R.layout.list_item_quote);

            if (cursor != null && this.cursor.moveToPosition(position)) {

                String symbol = cursor.getString(COL_STOCK_SYMBOL);
                String bid_price = cursor.getString(COL_STOCK_BID_PRICE);
                String stock_change = cursor.getString(COL_STOCK_PERCENT_CHANGE);

                remoteViews.setTextViewText(R.id.stock_symbol, symbol);
                remoteViews.setTextViewText(R.id.bid_price, bid_price);
                remoteViews.setTextViewText(R.id.change, stock_change);

                if (cursor.getInt(COL_STOCK_CHANGE) == 1) {
                    remoteViews.setInt(R.id.change, "setBackgroundResource", R.drawable.percent_change_pill_green);
                } else {
                    remoteViews.setInt(R.id.change, "setBackgroundResource", R.drawable.percent_change_pill_red);
                }

                Bundle bundle = new Bundle();
                bundle.putString(StockHawkWidgetProvider.EXTRA_SYMBOL, cursor.getString(COL_STOCK_SYMBOL));
                bundle.putString(StockHawkWidgetProvider.EXTRA_BID_PRICE, cursor.getString(COL_STOCK_BID_PRICE));
                bundle.putString(StockHawkWidgetProvider.EXTRA_PERCENT_CHANGE, cursor.getString(COL_STOCK_PERCENT_CHANGE));
                bundle.putString(StockHawkWidgetProvider.EXTRA_CHANGE, cursor.getString(COL_STOCK_CHANGE));
                bundle.putString(StockHawkWidgetProvider.EXTRA_IS_UP, cursor.getString(COL_STOCK_IS_UP));

                Intent intent = new Intent();
                intent.putExtras(bundle);
                remoteViews.setOnClickFillInIntent(R.id.linearLayout, intent);

            }

            return remoteViews;
        }

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public long getItemId(int position) {
            return this.cursor.getInt(0);
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }
    }
}
