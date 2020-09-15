package com.luminesim.futureplanner.ui.main;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
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
import com.google.android.gms.common.Feature;
import com.google.android.material.tabs.TabLayout;
import com.luminesim.futureplanner.R;
import com.luminesim.futureplanner.db.EntityParameter;
import com.luminesim.futureplanner.db.EntityRepository;
import com.luminesim.futureplanner.purchases.CanNavigateToStore;
import com.luminesim.futureplanner.purchases.FeatureSet;
import com.luminesim.futureplanner.simulation.EntityWithFundsSimulation;
import com.luminesim.futureplanner.simulation.SimpleIndividualIncomeSimulation;
import com.luminesim.futureplanner.ui.main.SectionsPagerAdapter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ResultsActivity extends AppCompatActivity implements CanNavigateToStore {

    private ViewPager mViewPager;

    @Override
    public void navigateToStore(FeatureSet featureFilter) {
        mViewPager.setCurrentItem(StoreFragment.PAGE_INDEX - 1);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_ui);
        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());
        mViewPager = findViewById(R.id.view_pager);
        mViewPager.setAdapter(sectionsPagerAdapter);
        TabLayout tabs = findViewById(R.id.tabs);
        tabs.setupWithViewPager(mViewPager);
        PageViewModel model = new ViewModelProvider(this).get(PageViewModel.class);
        tabs.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager) {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                Log.i("PageViewModel", "Tab selected" + tab.getPosition());
                model.setIndex(tab.getPosition()+1);
                if (tab.getPosition()+1 == ResultChartAndButtonsFragment.PAGE_INDEX) {
                    model.getSimulationRunFlag().postValue(true);
                }
                super.onTabSelected(tab);
            }
        });
        new ViewModelProvider(this).get(PageViewModel.class).setIndex(ResultChartAndButtonsFragment.PAGE_INDEX);
    }

    /**
     * Resumes the activity, re-running the simulation if facts have changed.
     */
    @Override
    protected void onResume() {
        super.onResume();
    }
}