package com.luminesim.futureplanner.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.luminesim.futureplanner.FactListActivity;
import com.luminesim.futureplanner.R;
import com.luminesim.futureplanner.Category;

/**
 * A placeholder fragment containing a simple view.
 */
public class FactListActivityLauncher extends Fragment {

    private static final String ARG_SECTION_NUMBER = "section_number";

    private PageViewModel pageViewModel;

    public static FactListActivityLauncher newInstance(int index) {
        FactListActivityLauncher fragment = new FactListActivityLauncher();
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
        View root = inflater.inflate(R.layout.fragment_result_chart_and_buttons, container, false);
        pageViewModel.getText().observe(getViewLifecycleOwner(), s -> {

            Log.i(getClass().getCanonicalName(), "Entity ID in fragment is " + getActivity().getIntent().getExtras());

            container.findViewById(R.id.buttonIncome).setOnClickListener(view -> {
                Intent intent = new Intent(getContext(), FactListActivity.class);
                intent.putExtra(FactListActivity.LIST_TITLE, R.string.button_income);
                intent.putExtra(FactListActivity.LIST_SELECTION, Category.Income);
                intent.putExtra(getString(R.string.extra_entity_uid), getActivity().getIntent().getLongExtra(getString(R.string.extra_entity_uid), 0l));
                startActivity(intent);
            });

            container.findViewById(R.id.buttonExpenses).setOnClickListener(view -> {
                Intent intent = new Intent(getContext(), FactListActivity.class);
                intent.putExtra(FactListActivity.LIST_TITLE, R.string.button_expenses);
                intent.putExtra(FactListActivity.LIST_SELECTION, Category.Expenses);
                intent.putExtra(getString(R.string.extra_entity_uid), getActivity().getIntent().getLongExtra(getString(R.string.extra_entity_uid), 0l));
                startActivity(intent);
            });


            // Load ads.
            AdView mAdView = container.findViewById(R.id.adView);
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
        });
        return root;
    }
}