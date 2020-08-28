package com.luminesim.futureplanner.ui.main;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.luminesim.futureplanner.Category;
import com.luminesim.futureplanner.R;
import com.luminesim.futureplanner.db.EntityRepository;
import com.luminesim.futureplanner.simulation.EntityWithFundsSimulation;
import com.luminesim.futureplanner.simulation.SimpleIndividualIncomeSimulation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link StoreFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class StoreFragment extends Fragment {

    private static final String ARG_SECTION_NUMBER = "section_number";

    private PageViewModel pageViewModel;

    public static StoreFragment newInstance(int index) {
        StoreFragment fragment = new StoreFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_SECTION_NUMBER, index);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pageViewModel = ViewModelProviders.of(this).get(PageViewModel.class);
        int index = 1;
        if (getArguments() != null) {
            index = getArguments().getInt(ARG_SECTION_NUMBER);
        }
        pageViewModel.setIndex(index);
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_store, container, false);
        pageViewModel.getText().observe(getViewLifecycleOwner(), s -> {

//            container.findViewById(R.id.buttonIncome).setOnClickListener(view -> {
//                Intent intent = new Intent(getContext(), FactListActivity.class);
//                intent.putExtra(FactListActivity.LIST_TITLE, R.string.button_income);
//                intent.putExtra(FactListActivity.LIST_SELECTION, Category.Income);
//                intent.putExtra(getString(R.string.extra_entity_uid), getActivity().getIntent().getLongExtra(getString(R.string.extra_entity_uid), 0l));
//                startActivity(intent);
//            });
        });

        // Add a bunch of test items.
        for (int i = 0; i < 3; i += 1) {
            View toAdd = getLayoutInflater().inflate(R.layout.view_store_card, null);
            ((TextView)toAdd.findViewById(R.id.labelStoreItemDescription)).setText("Test Description " + i);
            ((TextView)toAdd.findViewById(R.id.labelStoreItemName)).setText("Test Name " + i);
            ((TextView)toAdd.findViewById(R.id.labelStoreItemPrice)).setText("Test Price " + i);
            ((LinearLayout)root.findViewById(R.id.layoutStoreCards)).addView(toAdd);
        }


        return root;
    }
}