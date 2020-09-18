package com.luminesim.futureplanner.ui.main;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.ads.MobileAds;
import com.luminesim.futureplanner.R;
import com.luminesim.futureplanner.db.Entity;
import com.luminesim.futureplanner.db.EntityParameter;
import com.luminesim.futureplanner.db.EntityRepository;
import com.luminesim.futureplanner.db.EntityWithParameters;
import com.luminesim.futureplanner.purchases.FeatureManager;
import com.luminesim.futureplanner.simulation.SimpleIndividualIncomeSimulation;

import java.util.Arrays;

/**
 * Shows a splashscreen and creates the default entity if it doesn't exist.
 */
public class LandingActivity extends AppCompatActivity {
    private EntityRepository mEntities;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);

        // Launch the feature manager
//        Intent intent = new Intent(this, FeatureManager.class);
//        bindService(intent, new ServiceConnection() {
//            @Override
//            public void onServiceConnected(ComponentName name, IBinder service) {
//                name.
//            }
//
//            @Override
//            public void onServiceDisconnected(ComponentName name) {
//
//            }
//        }, Service.BIND_AUTO_CREATE);

        // Set up monetization.
        MobileAds.initialize(this, status -> {});

        // Create the entity repo.
        mEntities = new EntityRepository(getApplicationContext());

        // Create the first entity, if needed, then launch the main app.
        mEntities.getEntities().observe(this, data -> {

            mEntities.printAll();

            // No entity? Create it.
            if (data.isEmpty()) {
                Log.i("Setup", "No entities. Adding a user.");
                EntityParameter[] parameters = {
                        new EntityParameter(SimpleIndividualIncomeSimulation.PARAMETER_INITIAL_FUNDS, "0.00")
                };
                EntityWithParameters ewp = new EntityWithParameters();
                ewp.setEntity(Entity.builder()
                        .name("User")
                        .type(SimpleIndividualIncomeSimulation.ENTITY_TYPE)
                        .build());
                ewp.setParameters(Arrays.asList(parameters));
                mEntities.insert(ewp);
            }
            else {

                // We're done.
                finish();

                // Start the new activity.
                Entity user = data.get(0).getEntity();
                Intent launcher = new Intent(this, ResultsActivity.class);
                launcher.putExtra(getString(R.string.extra_entity_uid), user.getUid());
                startActivity(launcher);
            }
        });
    }
}