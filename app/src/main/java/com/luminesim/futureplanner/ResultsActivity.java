package com.luminesim.futureplanner;

import android.graphics.Color;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.tabs.TabLayout;
import com.luminesim.futureplanner.simulation.CanadianIndividualIncomeSimulation;
import com.luminesim.futureplanner.ui.main.SectionsPagerAdapter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ResultsActivity extends AppCompatActivity {

    /**
     * Boolean extra.
     * If true, the simulation's facts have changed, warranting a re-run.
     */
    public static final String EXTRA_SIMULATION_FACTS_CHANGED = "com.luminesim.futureplanner.EXTRA_SIMULATION_FACTS_CHANGED";
    private CanadianIndividualIncomeSimulation mSimulation;
    private ExecutorService mRunner = Executors.newFixedThreadPool(1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_ui);
        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());
        ViewPager viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(sectionsPagerAdapter);
        TabLayout tabs = findViewById(R.id.tabs);
        tabs.setupWithViewPager(viewPager);
    }

    /**
     * Resumes the activity, re-running the simulation if facts have changed.
     */
    @Override
    protected void onResume() {
        super.onResume();

        // Run the simulation.
        runSimulation(getIntent().getLongExtra(getString(R.string.extra_entity_uid), 0l));
    }

    /**
     * Runs the simulation for the given entity.
     *
     * @param entityUid
     */
    private void runSimulation(long entityUid) {
        mSimulation = new CanadianIndividualIncomeSimulation(10_000.0, this, entityUid);
        mRunner.submit(() -> {
            mSimulation.run();
            LineData data = getData(mSimulation);
            runOnUiThread(() -> {
                // Set up the chart and its  data.
                LineChart chart = findViewById(R.id.chartArea);
                chart.setData(data);

                // Fix the legend and description.
                chart.getDescription().setText("");

                // Update the Y scale to be a sensible size, defaulting to zero as the ymin
                // for a consistent scale. Also, remove the right axis as it adds no value.
                chart.getAxisLeft().setAxisMaximum((float) Math.max(100, data.getYMax() * 1.05f));
                chart.getAxisLeft().setAxisMinimum((float) Math.min(0, data.getYMin() - data.getYMin() * 0.05));
                chart.getAxisRight().setAxisMaximum(0);
                chart.getAxisRight().setAxisMinimum(0);

                // Update the X axis to use dates.
                chart.getXAxis().setValueFormatter(
                        new ValueFormatter() {
                            @Override
                            public String getAxisLabel(float value, AxisBase axis) {
                                return LocalDate.ofEpochDay((long) value).toString();
                            }
                        }
                );
                chart.getXAxis().setLabelRotationAngle(-45);
                chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);

                // Redraw.
                chart.invalidate();
            });
        });
    }

    private LineData getData(CanadianIndividualIncomeSimulation results) {
        // Get the data for the dataset.
        List<Entry> entries = new ArrayList<>(results.getFundsDataset().size());
        results.getFundsDataset().forEach((date, funds) -> entries.add(new Entry((float) date.toEpochDay(), funds.floatValue())));

        // Make it pretty.
        LineDataSet set1 = new LineDataSet(entries, getString(R.string.chart_legend_funds));
        set1.setAxisDependency(YAxis.AxisDependency.LEFT);
        set1.setColor(ColorTemplate.getHoloBlue());
        set1.setValueTextColor(ColorTemplate.getHoloBlue());
        set1.setLineWidth(3.0f);
        set1.setDrawCircles(false);
        set1.setDrawValues(false);
        set1.setFillAlpha(65);
        set1.setFillColor(ColorTemplate.getHoloBlue());
        set1.setHighLightColor(Color.rgb(244, 117, 117));
        set1.setDrawCircleHole(false);


        LineData data = new LineData(set1);
        data.setValueTextColor(Color.WHITE);
        data.setValueTextSize(9f);
        return data;
    }
}