package com.sam_chordas.android.stockhawk.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.db.chart.model.LineSet;
import com.db.chart.view.AxisController;
import com.db.chart.view.LineChartView;
import com.sam_chordas.android.stockhawk.R;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class DetailsActivity extends AppCompatActivity {

    private LineChartView lineChart;
    private TextView dataStatus;
    private String companySymbol;
    private String companyName;
    private ArrayList<String> labels;
    private ArrayList<Float> values;

//    private final String[] mLabels= {"", "", "", "", "START", "", "", "", "", "",
//            "", "", "", "", "", "", "", "", "", "",
//            "", "", "", "", "", "", "", "", "", "",
//            "", "", "", "", "", "FINISH", "", "", "", ""};
//    private final float[][] mValues = {
//            {35f, 37f, 47f, 49f, 43f,46f, 80f, 83f, 65f, 68f,
//                    28f, 55f, 58f, 50f, 53f, 53f, 57f, 48f, 50f, 53f,
//                    54f,25f, 27f, 35f, 37f, 35f, 80f, 82f, 55f, 59f,
//                    85f, 82f, 60f, 55f, 63f, 65f, 58f, 60f, 63f, 60f},
//            {85f, 85f, 85f, 85f, 85f, 85f, 85f, 85f, 85f, 85f,
//                    85f, 85f, 85f, 85f, 85f, 85f, 85f, 85f, 85f, 85f,
//                    85f, 85f, 85f, 85f, 85f, 85f, 85f, 85f, 85f, 85f,
//                    85f, 85f, 85f, 85f, 85f, 85f, 85f, 85f, 85f, 85f}};

    // Activity life cycle
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_line_graph);

        lineChart = (LineChartView) findViewById(R.id.linechart);
        dataStatus = (TextView)findViewById(R.id.data_status);
        companySymbol = getIntent().getStringExtra("symbol_name");

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(companyName);

        if (savedInstanceState == null) {
            GetDetailsFromSever();
        }
    }

    // Save/Restore activity state
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if(values != null||values.size() > 0){
            outState.putString("company_name", companyName);
            outState.putStringArrayList("labels", labels);

            float[] valuesArray = new float[values.size()];
            for (int i = 0; i < valuesArray.length; i++) {
                valuesArray[i] = values.get(i);
            }
            outState.putFloatArray("values", valuesArray);
        }

    }
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null && savedInstanceState.containsKey("company_name")) {

            companyName = savedInstanceState.getString("company_name");
            setTitle(companyName);
            labels = savedInstanceState.getStringArrayList("labels");
            values = new ArrayList<>();

            float[] valuesArray = savedInstanceState.getFloatArray("values");
            for (float points : valuesArray) {
                values.add(points);
            }
            RefreshLayout();
        }
        super.onRestoreInstanceState(savedInstanceState);
    }

    // Home button click
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else {
            return false;
        }
    }

    private void GetDetailsFromSever() {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("http://chartapi.finance.yahoo.com/instrument/1.0/" + companySymbol + "/chartdata;type=quote;range=1y/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Response response) throws IOException {
                if (response.code() == 200) {
                    try {

                        String resultstr = response.body().string();
                        if (resultstr.startsWith("finance_charts_json_callback( ")) {
                            resultstr = resultstr.substring(resultstr.indexOf("(") + 1, resultstr.lastIndexOf(")"));
                        }

                        JSONObject object = new JSONObject(resultstr);
                        companyName = object.getJSONObject("meta").getString("Company-Name");
                        labels = new ArrayList<>();
                        values = new ArrayList<>();
                        JSONArray series = object.getJSONArray("series");
                        for (int i = 0; i < series.length(); i++) {
                            JSONObject obeject = series.getJSONObject(i);
                            SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
                            String date = android.text.format.DateFormat.
                                    getMediumDateFormat(getApplicationContext()).
                                    format(format.parse(obeject.getString("Date")));
                            labels.add(date);
                            values.add(Float.parseFloat(obeject.getString("close")));
                        }

                        RefreshLayout();
                    } catch (Exception e) {
                        OnFetchDataFailed();
                        e.printStackTrace();
                    }
                } else {
                    OnFetchDataFailed();
                }
            }

            @Override
            public void onFailure(Request request, IOException e) {
                OnFetchDataFailed();
            }
        });
    }

    private void OnFetchDataFailed() {
        DetailsActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                lineChart.setVisibility(View.INVISIBLE);
                dataStatus.setVisibility(View.VISIBLE);
            }
        });
    }

    private void RefreshLayout() {
        DetailsActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                lineChart.setVisibility(View.VISIBLE);
                dataStatus.setVisibility(View.INVISIBLE);
;
                float[] data = new float[values.size()];
                String[] strings = new String[labels.size()];



                for (int i = 0; i< labels.size();i++) {
                    strings[i] = labels.get(i);
                    data[i] = values.get(i);
                }

                LineSet dataset = new LineSet(strings,data);
                dataset.setSmooth(true).
                        setColor(Color.parseColor("#b3b5bb")).
                        setFill(Color.parseColor("#56B7F1"));

                lineChart.setBackgroundColor(Color.WHITE);
                lineChart.setYLabels(AxisController.LabelPosition.NONE);
                lineChart.setXAxis(false);
                lineChart.setYAxis(false);

                lineChart.addData(dataset);
                lineChart.show();


            }
        });
    }

}
