package com.example.myfirstapp;

import android.content.Intent;
import android.os.Bundle;

import com.example.myfirstapp.db.Entity;
import com.example.myfirstapp.db.EntityDatabase;
import com.example.myfirstapp.db.EntityRepository;
import com.example.myfirstapp.db.EntityWithFacts;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import androidx.lifecycle.LiveData;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.example.myfirstapp.ui.main.SectionsPagerAdapter;

import java.util.List;
import java.util.Locale;

import javax.xml.transform.Result;

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
            }
            else {
                // No entity ID? Get the first one.
                if (!getIntent().hasExtra(getString(R.string.extra_entity_uid))) {
                    Log.i("Setup", "No entity ID ready for use. Acquiring it.");
                    getIntent().putExtra(getString(R.string.extra_entity_uid), list.get(0).getEntity().getUid());
                    Log.i("Setup", "Entity ID is " + list.get(0).getEntity().getUid());
                }
            }
        });
    }
}