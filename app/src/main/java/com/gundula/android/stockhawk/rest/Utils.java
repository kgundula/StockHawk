package com.gundula.android.stockhawk.rest;

import android.content.ContentProviderOperation;

import com.gundula.android.stockhawk.data.QuoteColumns;
import com.gundula.android.stockhawk.data.QuoteProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by sam_chordas on 10/8/15.
 */
public class Utils {

    private static String LOG_TAG = Utils.class.getSimpleName();

    public final static String BASE_URL = "http://chartapi.finance.yahoo.com/instrument/1.0/";
    public final static String END_URL = "/chartdata;type=quote;range=1y/json";

    public static boolean showPercent = true;

    public static ArrayList quoteJsonToContentVals(String JSON) throws JSONException {
        ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();
        JSONObject jsonObject = null;
        JSONArray resultsArray = null;
        jsonObject = new JSONObject(JSON);
        if (jsonObject != null && jsonObject.length() != 0) {
            jsonObject = jsonObject.getJSONObject("query");
            int count = Integer.parseInt(jsonObject.getString("count"));
            if (count == 1) {
                jsonObject = jsonObject.getJSONObject("results")
                        .getJSONObject("quote");
                    batchOperations.add(buildBatchOperation(jsonObject));
            } else {
                resultsArray = jsonObject.getJSONObject("results").getJSONArray("quote");

                if (resultsArray != null && resultsArray.length() != 0) {
                    for (int i = 0; i < resultsArray.length(); i++) {
                        jsonObject = resultsArray.getJSONObject(i);
                        batchOperations.add(buildBatchOperation(jsonObject));
                    }
                }
            }
        }
        return batchOperations;
    }

    public static String stockAllCaps(String stockName) {

        String[] arr = stockName.split(" ");
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < arr.length; i++) {
            sb.append(arr[i].toUpperCase()).append(" ");
        }
        return sb.toString().trim();
    }

    public static String truncateBidPrice(String bidPrice) {
        try {
            bidPrice = String.format("%.2f", Float.parseFloat(bidPrice));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return bidPrice;
    }

    public static boolean checkSpaceOnStock(String stockName) {
        Pattern pattern = Pattern.compile("\\s");
        Matcher matcher = pattern.matcher(stockName);
        boolean found = matcher.find();
        return found;
    }


    public static String truncateChange(String change, boolean isPercentChange) {
        String weight = change.substring(0, 1);
        String ampersand = "";
        if (isPercentChange) {
            ampersand = change.substring(change.length() - 1, change.length());
            change = change.substring(0, change.length() - 1);
        }
        try {
            change = change.substring(1, change.length());
            double round = (double) Math.round(Double.parseDouble(change) * 100) / 100;
            change = String.format("%.2f", round);
            StringBuffer changeBuffer = new StringBuffer(change);
            changeBuffer.insert(0, weight);
            changeBuffer.append(ampersand);
            change = changeBuffer.toString();
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return change;
    }

    public static boolean isStockCommaSeparated(String stockName) {
        boolean stock_has_comma = false;
        if (stockName.contains(",")) {
            stock_has_comma = true;
        }
        return stock_has_comma;
    }

    public static ContentProviderOperation buildBatchOperation(JSONObject jsonObject) {
        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
                QuoteProvider.Quotes.CONTENT_URI);
        try {
            String change = jsonObject.getString("Change");
            builder.withValue(QuoteColumns.SYMBOL, jsonObject.getString("symbol"));
            builder.withValue(QuoteColumns.BIDPRICE, truncateBidPrice(jsonObject.getString("Bid")));
            builder.withValue(QuoteColumns.PERCENT_CHANGE, truncateChange(
                    jsonObject.getString("ChangeinPercent"), true));
            builder.withValue(QuoteColumns.CHANGE, truncateChange(change, false));
            builder.withValue(QuoteColumns.ISCURRENT, 1);
            if (change.charAt(0) == '-') {
                builder.withValue(QuoteColumns.ISUP, 0);
            } else {
                builder.withValue(QuoteColumns.ISUP, 1);
            }
            return builder.build();

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
