package com.gundula.android.stockhawk.ui;

import android.app.LoaderManager;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.InputType;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.gundula.android.stockhawk.R;
import com.gundula.android.stockhawk.data.QuoteColumns;
import com.gundula.android.stockhawk.data.QuoteProvider;
import com.gundula.android.stockhawk.rest.QuoteCursorAdapter;
import com.gundula.android.stockhawk.rest.RecyclerViewItemClickListener;
import com.gundula.android.stockhawk.rest.Utils;
import com.gundula.android.stockhawk.service.StockIntentService;
import com.gundula.android.stockhawk.service.StockTaskService;
import com.gundula.android.stockhawk.touch_helper.SimpleItemTouchHelperCallback;
import com.gundula.android.stockhawk.widget.StockHawkWidgetService;

import butterknife.Bind;
import butterknife.ButterKnife;

import static com.gundula.android.stockhawk.rest.Utils.checkSpaceOnStock;

public class MyStocksActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */

    private Intent mServiceIntent;
    private ItemTouchHelper mItemTouchHelper;
    private static final int CURSOR_LOADER_ID = 0;
    private QuoteCursorAdapter mCursorAdapter;
    private Context mContext;
    private Cursor mCursor;
    boolean isConnected;

    @Bind(R.id.recyclerView)
    RecyclerView recyclerView;
    @Bind(R.id.fab)
    FloatingActionButton fab;
    @Bind(R.id.coordinator_layout)
    CoordinatorLayout coordinator_layout;
    @Bind(R.id.toolbar)
    Toolbar toolbar;
    @Bind(R.id.empty_state_view)
    TextView empty_state_view;

    public static final int COL_STOCK_ID = 0;
    public static final int COL_STOCK_SYMBOL = 1;
    public static final int COL_STOCK_BID_PRICE = 2;
    public static final int COL_STOCK_PERCENT_CHANGE = 3;
    public static final int COL_STOCK_CHANGE = 4;
    public static final int COL_STOCK_IS_UP = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_stocks);
        mContext = this;
        ButterKnife.bind(this);

        toolbar.setTitle(getResources().getString(R.string.app_name));
        setSupportActionBar(toolbar);

        ConnectivityManager cm =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        // The intent service is for executing immediate pulls from the Yahoo API
        // GCMTaskService can only schedule tasks, they cannot execute immediately
        mServiceIntent = new Intent(this, StockIntentService.class);
        if (savedInstanceState == null) {
            // Run the initialize task service so that some stocks appear upon an empty database
            mServiceIntent.putExtra("tag", "init");
            if (isConnected) {
                startService(mServiceIntent);
            } else {
                networkToast();
            }
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);

        mCursorAdapter = new QuoteCursorAdapter(this, null);
        recyclerView.addOnItemTouchListener(new RecyclerViewItemClickListener(this,
                new RecyclerViewItemClickListener.OnItemClickListener() {

                    @Override
                    public void onItemClick(View v, int position) {

                        if (mCursor.moveToPosition(position)) {
                            Intent intent = new Intent(mContext, StockDetailsActivity.class);
                            intent.putExtra(mCursor.getColumnName(COL_STOCK_SYMBOL), mCursor.getString(COL_STOCK_SYMBOL));
                            intent.putExtra(mCursor.getColumnName(COL_STOCK_BID_PRICE), mCursor.getString(COL_STOCK_BID_PRICE));
                            intent.putExtra(mCursor.getColumnName(COL_STOCK_PERCENT_CHANGE), mCursor.getString(COL_STOCK_PERCENT_CHANGE));
                            intent.putExtra(mCursor.getColumnName(COL_STOCK_CHANGE), mCursor.getString(COL_STOCK_CHANGE));
                            intent.putExtra(mCursor.getColumnName(COL_STOCK_IS_UP), mCursor.getString(COL_STOCK_IS_UP));
                            startActivity(intent);
                        }

                    }

                }));

        recyclerView.setAdapter(mCursorAdapter);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (isConnected) {
                    new MaterialDialog.Builder(mContext).title(R.string.symbol_search)
                            .content(R.string.content_test)
                            .inputType(InputType.TYPE_CLASS_TEXT)
                            .input(R.string.input_hint, R.string.input_prefill, new MaterialDialog.InputCallback() {
                                @Override
                                public void onInput(MaterialDialog dialog, CharSequence input) {
                                    // On FAB click, receive user input. Make sure the stock doesn't already exist
                                    // in the DB and proceed accordingly
                                    Cursor c = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                                            new String[]{QuoteColumns.SYMBOL}, QuoteColumns.SYMBOL + "= ?",
                                            new String[]{input.toString()}, null);


                                    if (c.getCount() != 0) {
                                        displayToast(getResources().getString(R.string.stock_exist));
                                        return;
                                    } else {
                                        boolean contains_space = checkSpaceOnStock(input.toString());
                                        if (contains_space) {
                                            displayToast(getResources().getString(R.string.stock_name_space));
                                        } else {
                                            // Add the stock to DB
                                            mServiceIntent.putExtra("tag", "add");
                                            mServiceIntent.putExtra("symbol", input.toString());
                                            startService(mServiceIntent);
                                        }
                                    }
                                }
                            })
                            .show();
                } else {
                    networkToast();
                }

            }
        });

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mCursorAdapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(recyclerView);

        if (isConnected) {
            long period = 3600L;
            long flex = 10L;
            String periodicTag = "periodic";

            // create a periodic task to pull stocks once every hour after the app has been opened. This
            // is so Widget data stays up to date.
            PeriodicTask periodicTask = new PeriodicTask.Builder()
                    .setService(StockTaskService.class)
                    .setPeriod(period)
                    .setFlex(flex)
                    .setTag(periodicTag)
                    .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                    .setRequiresCharging(false)
                    .build();
            // Schedule task with tag "periodic." This ensure that only the stocks present in the DB
            // are updated.
            GcmNetworkManager.getInstance(this).schedule(periodicTask);
        }
    }

    public void displayToast(String message) {
        Toast toast =
                Toast.makeText(MyStocksActivity.this, message,
                        Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, Gravity.CENTER, 0);
        toast.show();
    }


    @Override
    public void onResume() {
        super.onResume();
        getLoaderManager().restartLoader(CURSOR_LOADER_ID, null, this);
        emptyView();
    }

    public void networkToast() {
        Toast.makeText(mContext, getString(R.string.network_toast), Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_stocks, menu);
        //restoreActionBar();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        if (id == R.id.refresh_stock) {
            //Force Stock Update
            mServiceIntent = new Intent(this, StockIntentService.class);
            mServiceIntent.putExtra("tag", "init");
            if (isConnected) {
                startService(mServiceIntent);
            } else {
                networkToast();
            }
        }

        if (id == R.id.action_change_units) {
            // this is for changing stock changes from percent value to dollar value
            Utils.showPercent = !Utils.showPercent;
            this.getContentResolver().notifyChange(QuoteProvider.Quotes.CONTENT_URI, null);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This narrows the return to only the stocks that are most current.
        return new CursorLoader(this, QuoteProvider.Quotes.CONTENT_URI,
                new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                        QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
                QuoteColumns.ISCURRENT + " = ?",
                new String[]{"1"},
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mCursorAdapter.swapCursor(data);
        mCursor = data;

        emptyView();

        updateStockWidget();
    }

    private void updateStockWidget() {

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(mContext.getApplicationContext());
        int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(this, StockHawkWidgetService.class));
        if (ids.length > 0) {
            appWidgetManager.notifyAppWidgetViewDataChanged(ids, R.id.stock_widget_layout);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCursorAdapter.swapCursor(null);
    }

    public void emptyView() {
        if (mCursorAdapter.getItemCount() <= 0) {

            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
            @StockTaskService.StockStatus int stockStatus = sp.getInt(getString(R.string.stock_status), -1);
            String message = getString(R.string.empty_data) + " ";


            switch (stockStatus) {
                case StockTaskService.STATUS_OK:
                    message += getString(R.string.status_ok);
                    break;
                case StockTaskService.STATUS_JSON_ERROR:
                    message += getString(R.string.json_error);
                    break;
                case StockTaskService.STATUS_NO_NETWORK:
                    message += getString(R.string.no_network_error);
                    break;
                case StockTaskService.STATUS_SERVER_DOWN:
                    message += getString(R.string.server_down_error);
                    break;
                case StockTaskService.STATUS_SERVER_ERROR:
                    message += getString(R.string.server_error);
                    break;
                case StockTaskService.STATUS_UNKNOWN:
                    message += getString(R.string.unknown_error);
                    break;
                default:
                    message += getString(R.string.no_network_error);
                    break;
            }

            empty_state_view.setText(message);
            recyclerView.setVisibility(View.INVISIBLE);
            empty_state_view.setVisibility(View.VISIBLE);

        } else {
            recyclerView.setVisibility(View.VISIBLE);
            empty_state_view.setVisibility(View.INVISIBLE);
        }
    }

}
