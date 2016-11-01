package com.gundula.android.stockhawk.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.gundula.android.stockhawk.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

import static com.gundula.android.stockhawk.rest.Utils.BASE_URL;
import static com.gundula.android.stockhawk.rest.Utils.END_URL;

public class StockDetailsActivity extends AppCompatActivity {

    @Bind(R.id.toolbar)
    Toolbar toolbar;
    @Bind(R.id.symbol)
    TextView stock_symbol;
    @Bind(R.id.bid_price)
    TextView stock_bid_price;
    @Bind(R.id.percent_change)
    TextView stock_percent_change;
    @Bind(R.id.change)
    TextView stock_change;
    @Bind(R.id.is_up)
    TextView stock_is_up;
    @Bind(R.id.coordinator_layout)
    CoordinatorLayout coordinator_layout;

    @Bind(R.id.linechart)
    LineChart lineChart;
    @Bind(R.id.empty_state_view)
    TextView empty_state_view;

    Context context;

    String symbol = "";
    String bid_price = "";
    String percent_change = "";
    String change = "";
    String is_up;


    @Retention(RetentionPolicy.SOURCE)
    @IntDef({STATUS_OK, STATUS_SERVER_ERROR, STATUS_NO_NETWORK, STATUS_JSON_ERROR, STATUS_UNKNOWN, STATUS_SERVER_DOWN})
    public @interface StockStatus {
    }

    private static final String stock_series = "series";
    private static final String stock_date = "Date";
    private static final String stock_close = "close";


    public static final int STATUS_OK = 0;
    public static final int STATUS_SERVER_ERROR = 1;
    public static final int STATUS_NO_NETWORK = 2;
    public static final int STATUS_JSON_ERROR = 3;
    public static final int STATUS_UNKNOWN = 4;
    public static final int STATUS_SERVER_DOWN = 5;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_details);
        context = getApplicationContext();
        ButterKnife.bind(this);

        toolbar.setTitle(getResources().getString(R.string.app_name));
        setSupportActionBar(toolbar);

        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            symbol = bundle.getString("symbol");
            bid_price = bundle.getString("bid_price");
            percent_change = bundle.getString("percent_change");
            change = bundle.getString("change");
            is_up = bundle.getString("is_up");

            stock_symbol.setText(symbol);
            stock_bid_price.setText(bid_price);
            stock_percent_change.setText(percent_change);
            stock_change.setText(change);

            String stock_description = "";
            int sdk = Build.VERSION.SDK_INT;

            if ("1".equals(is_up)) {
                stock_description = getResources().getString(R.string.is_stock_up) + " " + change;
                if (sdk < Build.VERSION_CODES.JELLY_BEAN) {
                    stock_percent_change.setBackgroundDrawable(
                            context.getResources().getDrawable(R.drawable.percent_change_pill_green));
                } else {
                    stock_percent_change.setBackground(
                            context.getResources().getDrawable(R.drawable.percent_change_pill_green));
                }
            } else {
                stock_description = getResources().getString(R.string.is_stock_down) + " " + change;
                if (sdk < Build.VERSION_CODES.JELLY_BEAN) {
                    stock_percent_change.setBackgroundDrawable(
                            context.getResources().getDrawable(R.drawable.percent_change_pill_red));
                } else {
                    stock_percent_change.setBackground(
                            context.getResources().getDrawable(R.drawable.percent_change_pill_red));
                }
            }
            stock_is_up.setText(stock_description);

        }

        getHistoricalStockData(symbol);

    }

    public void getHistoricalStockData(final String symbol) {
        String URL = BASE_URL + symbol + END_URL;
        final StringRequest request = new StringRequest(Request.Method.GET, URL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                try {

                    String json = response.substring(response.indexOf("(") + 1, response.lastIndexOf(")"));
                    JSONObject stock_details_json = new JSONObject(json);
                    JSONArray series_data = stock_details_json.getJSONArray(stock_series);
                    List<Entry> entries = new ArrayList<Entry>();
                    for (int i = 0; i < series_data.length(); i += 10) {
                        JSONObject singleObject = series_data.getJSONObject(i);
                        float close = (float) singleObject.getDouble(stock_close);
                        float date = (float) i;
                        Entry stock = new Entry(date, close);
                        entries.add(stock);
                    }

                    LineDataSet dataSet = new LineDataSet(entries, "Stock Price ($)");
                    LineData lineData = new LineData(dataSet);
                    Description description = new Description();
                    String desc = symbol+"\'s "+getResources().getString(R.string.stock_details_history);
                    description.setText(desc);
                    lineChart.setContentDescription(symbol);
                    lineChart.setDescription(description);
                    lineChart.setData(lineData);
                    lineChart.setVisibility(View.VISIBLE);
                    lineChart.invalidate();
                    empty_state_view.setVisibility(View.INVISIBLE);

                } catch (JSONException e) {
                    e.printStackTrace();
                    setErrorMessageStatus(context, STATUS_JSON_ERROR);
                    emptyViewError();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (error instanceof TimeoutError || error instanceof NoConnectionError) {
                    setErrorMessageStatus(context, STATUS_NO_NETWORK);
                } else if (error instanceof ServerError) {
                    setErrorMessageStatus(context, STATUS_SERVER_ERROR);
                } else if (error instanceof NetworkError) {
                    setErrorMessageStatus(context, STATUS_UNKNOWN);
                } else if (error instanceof ParseError) {
                    setErrorMessageStatus(context, STATUS_SERVER_DOWN);
                }
                emptyViewError();

            }
        });
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(request);

    }

    public void setErrorMessageStatus(Context context, @StockDetailsActivity.StockStatus int stockStatus) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(context.getString(R.string.stock_status), stockStatus);
        editor.apply();
    }

    public void emptyViewError() {

        String errorMessage = getString(R.string.empty_data) + " ";
        @StockDetailsActivity.StockStatus int status = StockDetailsActivity.getStockStatus(this);
        switch (status) {
            case StockDetailsActivity.STATUS_SERVER_ERROR:
                errorMessage += getString(R.string.server_error);
                break;
            case StockDetailsActivity.STATUS_JSON_ERROR:
                errorMessage += getString(R.string.json_error);
                break;
            case StockDetailsActivity.STATUS_UNKNOWN:
                errorMessage += getString(R.string.unknown_error);
                break;
            case StockDetailsActivity.STATUS_SERVER_DOWN:
                errorMessage += getString(R.string.server_down_error);
                break;
            case StockDetailsActivity.STATUS_NO_NETWORK:
                errorMessage += getString(R.string.no_network_error);
                break;
            default:
                break;
        }

        empty_state_view.setText(errorMessage);
        lineChart.setVisibility(View.INVISIBLE);
        empty_state_view.setVisibility(View.VISIBLE);

    }

    @SuppressWarnings("ResourceType")
    static public
    @StockDetailsActivity.StockStatus
    int getStockStatus(Context c) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        return sp.getInt(c.getString(R.string.stock_status), StockDetailsActivity.STATUS_UNKNOWN);
    }

}
