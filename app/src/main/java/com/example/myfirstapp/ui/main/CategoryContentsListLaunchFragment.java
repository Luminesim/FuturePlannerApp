package com.example.myfirstapp.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.example.myfirstapp.CategoryContentsListActivity;
import com.example.myfirstapp.R;
import com.example.myfirstapp.Category;

/**
 * A placeholder fragment containing a simple view.
 */
public class CategoryContentsListLaunchFragment extends Fragment {

    private static final String ARG_SECTION_NUMBER = "section_number";

    private PageViewModel pageViewModel;

    public static CategoryContentsListLaunchFragment newInstance(int index) {
        CategoryContentsListLaunchFragment fragment = new CategoryContentsListLaunchFragment();
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
        View root = inflater.inflate(R.layout.fragment_results, container, false);
        pageViewModel.getText().observe(getViewLifecycleOwner(), s -> {

            Log.i(getClass().getCanonicalName(), "Entity ID in fragment is " + getActivity().getIntent().getExtras());

            container.findViewById(R.id.buttonIncome).setOnClickListener(view -> {
                Intent intent = new Intent(getContext(), CategoryContentsListActivity.class);
                intent.putExtra(CategoryContentsListActivity.LIST_TITLE, R.string.button_income);
                intent.putExtra(CategoryContentsListActivity.LIST_SELECTION, Category.Income);
                intent.putExtra(getString(R.string.extra_entity_uid), getActivity().getIntent().getLongExtra(getString(R.string.extra_entity_uid), 0l));
                startActivity(intent);
            });

            container.findViewById(R.id.buttonExpenses).setOnClickListener(view -> {
                Intent intent = new Intent(getContext(), CategoryContentsListActivity.class);
                intent.putExtra(CategoryContentsListActivity.LIST_TITLE, R.string.button_expenses);
                intent.putExtra(CategoryContentsListActivity.LIST_SELECTION, Category.Expenses);
                intent.putExtra(getString(R.string.extra_entity_uid), getActivity().getIntent().getLongExtra(getString(R.string.extra_entity_uid), 0l));
                startActivity(intent);
            });
        });
        return root;
    }
}