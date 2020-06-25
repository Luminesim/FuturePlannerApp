package com.luminesim.futureplanner;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.viewpager.widget.ViewPager;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.tabs.TabLayout;
import com.luminesim.futureplanner.db.Entity;
import com.luminesim.futureplanner.db.EntityRepository;
import com.luminesim.futureplanner.db.EntityWithFacts;
import com.luminesim.futureplanner.simulation.SimulationJob;
import com.luminesim.futureplanner.ui.main.SectionsPagerAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ResultsActivity extends AppCompatActivity {

    private EntityRepository mEntities;
    private SimulationJob mSimulation;
    private ExecutorService mRunner = Executors.newFixedThreadPool(1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);
        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());
        ViewPager viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(sectionsPagerAdapter);
        TabLayout tabs = findViewById(R.id.tabs);
        tabs.setupWithViewPager(viewPager);

        // Do setup if needed.
        tryInitializeDatabaseAndFirstEntity();
    }

    /**
     * Initializes the database if needed.
     */
    void tryInitializeDatabaseAndFirstEntity() {

        // TODO FIXME: This will introduce a race condition if the user presses Income/Expense before this result returns.
        mEntities = new EntityRepository(getApplicationContext());

//        mEntities.deleteAll();

        LiveData<List<EntityWithFacts>> entities = mEntities.getEntities();
        entities.observe(this, list -> {

            // No entities? Create a default one.
            if (list.isEmpty()) {
                Log.i("Setup", "No entities. Adding a user.");
                mEntities.insert(Entity.builder().name("User").build());
            } else {
                // No entity ID? Get the first one.
                long uid = 0l;
                if (!getIntent().hasExtra(getString(R.string.extra_entity_uid))) {
                    getIntent().putExtra(getString(R.string.extra_entity_uid), list.get(0).getEntity().getUid());
                    Log.i("Setup", "Entity ID is " + list.get(0).getEntity().getUid());
                    uid = list.get(0).getEntity().getUid();
                } else {
                    uid = getIntent().getLongExtra(getString(R.string.extra_entity_uid), 0l);
                }

                runSimulation(uid);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (getIntent().hasExtra(getString(R.string.extra_entity_uid))) {
            runSimulation(getIntent().getLongExtra(getString(R.string.extra_entity_uid), 0l));
        }
    }

    /**
     * Runs the simulation for the given entity.
     * @param entityUid
     */
    private void runSimulation(long entityUid) {
        mSimulation = new SimulationJob(this, entityUid);
        mRunner.submit(() -> {
            mSimulation.run();
            LineData data = getData(mSimulation);
            runOnUiThread(() -> {
                LineChart chart = findViewById(R.id.chartArea);
                chart.setData(data);
                chart.invalidate();
            });
            Log.i("SIMULATION COMPLETE!", "SIMULATION COMPLETE!");
        });
    }

    private LineData getData(SimulationJob results) {
        // Get the data for the dataset.
        List<Entry> entries = new ArrayList<>(results.getFundsDataset().size());
        results.getFundsDataset().forEach((date, funds) -> entries.add(new Entry((float)date.toEpochDay(), funds.floatValue())));

        // Make it pretty.
        LineDataSet set1 = new LineDataSet(entries, "Funds");
        set1.setAxisDependency(YAxis.AxisDependency.LEFT);
        set1.setColor(ColorTemplate.getHoloBlue());
        set1.setValueTextColor(ColorTemplate.getHoloBlue());
        set1.setLineWidth(1.5f);
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