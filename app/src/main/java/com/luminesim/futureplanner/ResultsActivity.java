package com.luminesim.futureplanner;

import android.os.Bundle;

import com.google.common.util.concurrent.ListenableFuture;
import com.luminesim.futureplanner.db.Entity;
import com.luminesim.futureplanner.db.EntityRepository;
import com.luminesim.futureplanner.db.EntityWithFacts;
import com.google.android.material.tabs.TabLayout;

import androidx.lifecycle.LiveData;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.Operation;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import android.util.Log;

import com.luminesim.futureplanner.simulation.SimulationWorker;
import com.luminesim.futureplanner.ui.main.SectionsPagerAdapter;

import java.util.List;
import java.util.concurrent.Executors;

public class ResultsActivity extends AppCompatActivity {

    private EntityRepository mEntities;

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

                // Run a simulation.
                WorkRequest simulationWorkRequest = new OneTimeWorkRequest
                        .Builder(SimulationWorker.class)
                        .setInputData(
                                new Data.Builder()
                                        .putLong(SimulationWorker.DATA_ENTITY_UID, uid).build())
                        .build();
                WorkManager
                        .getInstance(this)
                        .enqueue(simulationWorkRequest)
                        .getResult()
                        .addListener(() -> Log.i("SIMULATION DONE!", "Done"), Executors.newSingleThreadExecutor());
            }
        });
    }
}