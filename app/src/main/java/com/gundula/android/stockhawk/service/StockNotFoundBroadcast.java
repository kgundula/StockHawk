package com.gundula.android.stockhawk.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.gundula.android.stockhawk.R;

/**
 * Created by kgundula on 2016/11/01.
 */

public class StockNotFoundBroadcast extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context, context.getString(R.string.stock_not_found), Toast.LENGTH_SHORT).show();
    }
}
