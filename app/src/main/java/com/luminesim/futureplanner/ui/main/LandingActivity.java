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
import com.luminesim.futureplanner.Category;
import com.luminesim.futureplanner.R;
import com.luminesim.futureplanner.db.Entity;
import com.luminesim.futureplanner.db.EntityParameter;
import com.luminesim.futureplanner.db.EntityRepository;
import com.luminesim.futureplanner.db.EntityWithParameters;
import com.luminesim.futureplanner.models.ModelPackage;
import com.luminesim.futureplanner.models.ModelPackageManager;
import com.luminesim.futureplanner.models.ModelTemplate;
import com.luminesim.futureplanner.models.bassdiffusion.HasAdopterLifespan;
import com.luminesim.futureplanner.models.bassdiffusion.HasAdopterRetention;
import com.luminesim.futureplanner.models.bassdiffusion.HasConversionFromAds;
import com.luminesim.futureplanner.models.bassdiffusion.HasPotentialAdopters;
import com.luminesim.futureplanner.models.bassdiffusion.HasSpreadThroughContacts;
import com.luminesim.futureplanner.models.bassdiffusion.freemium.HasFreeAdopters;
import com.luminesim.futureplanner.models.bassdiffusion.freemium.HasPayingAdopters;
import com.luminesim.futureplanner.models.bassdiffusion.freemium.HasPercentChanceOfUsersBecomingPayingUsers;
import com.luminesim.futureplanner.models.bassdiffusion.freemium.IsCompleteFreemiumBassDiffusionModel;
import com.luminesim.futureplanner.models.bassdiffusion.freemium.IsPartialFreemiumBassDiffusionModel;
import com.luminesim.futureplanner.monad.MonadDatabase;
import com.luminesim.futureplanner.purchases.FeatureManager;
import com.luminesim.futureplanner.simulation.SimpleIndividualIncomeSimulation;

import java.util.Arrays;
import java.util.Collections;

/**
 * Shows a splashscreen and creates the default entity if it doesn't exist.
 */
public class LandingActivity extends AppCompatActivity {
    private EntityRepository mEntities;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);

        // Launch the package manager.

        // XXX HACK FIXME
        ModelPackageManager packageManager = new ModelPackageManager(MonadDatabase.getDatabase(getApplicationContext()));
        packageManager.enable(new ModelPackage(
                IsCompleteFreemiumBassDiffusionModel.class.getName(),
                IsCompleteFreemiumBassDiffusionModel.class,
                new ModelTemplate(
                        "FreemiumBassDiffusion",
                        "Adopter",
                        new ModelTemplate.AssetDefinition("Adopter"),
                        Arrays.asList(new ModelTemplate.SetupInstructions())
                ),
                Collections.emptyMap(),
                db -> {
                    db.add(
                            IsPartialFreemiumBassDiffusionModel.class.getName(),
                            IsPartialFreemiumBassDiffusionModel.create(),
                            Arrays.asList(Category.ModelDefinition),
                            "is a product",
                            "is a product",
                            getString(R.string.hint_freemium_bass_diffusion));
                    db.addNumericOneParameter(
                            HasFreeAdopters.class.getName(),
                            HasFreeAdopters.create(),
                            Category.ModelDefinition,
                            "has %s free adopters",
                            "has <number> free adopters",
                            getString(R.string.hint_freemium_bass_diffusion_has_free_adopters));
                    db.addNumericOneParameter(
                            HasPayingAdopters.class.getName(),
                            HasPayingAdopters.create(),
                            Category.ModelDefinition,
                            "has %s paying adopters",
                            "has <number> paying adopters",
                            getString(R.string.hint_freemium_bass_diffusion_has_paying_adopters));
                    db.addPercentOneParameter(
                            HasPercentChanceOfUsersBecomingPayingUsers.class.getName(),
                            HasPercentChanceOfUsersBecomingPayingUsers.create(),
                            Category.ModelDefinition,
                            "%s%% of new adopters pay for features",
                            "<percent> of new adopters pay for features",
                            getString(R.string.hint_freemium_bass_diffusion_has_percent_chance_of_paying_users));
                    db.addNumericOneParameter(
                            HasAdopterLifespan.class.getName(),
                            HasAdopterLifespan.create(),
                            Category.ModelDefinition,
                            "adopters use product for %s months on average",
                            "adopters use product for <number> months on average",
                            getString(R.string.hint_freemium_bass_diffusion_has_adopter_lifespan));
                    db.addPercentOneParameter(
                            HasAdopterRetention.class.getName(),
                            HasAdopterRetention.create(),
                            Category.ModelDefinition,
                            "%s%% adopters use product after first use",
                            "<percent> adopters use product after first use",
                            getString(R.string.hint_freemium_bass_diffusion_has_adopter_retention));
                    db.addPercentOneParameter(
                            HasConversionFromAds.class.getName(),
                            HasConversionFromAds.create(),
                            Category.ModelDefinition,
                            "ads convert %s%% of potential adopters each month",
                            "ads convert <percent> of potential adopters each month",
                            getString(R.string.hint_freemium_bass_diffusion_has_conversion_from_ads));
                    db.addNumericOneParameter(
                            HasPotentialAdopters.class.getName(),
                            HasPotentialAdopters.create(),
                            Category.ModelDefinition,
                            "has %s potential adopters",
                            "has <number> potential adopters",
                            getString(R.string.hint_freemium_bass_diffusion_has_potential_adopters));
                    db.addPercentOneParameter(
                            HasSpreadThroughContacts.class.getName(),
                            HasSpreadThroughContacts.create(),
                            Category.ModelDefinition,
                            "adopters convert up to %s potential adopters each month",
                            "adopters convert up to <number> potential adopters each month",
                            getString(R.string.hint_freemium_bass_diffusion_has_wom_conversion));
                }));

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