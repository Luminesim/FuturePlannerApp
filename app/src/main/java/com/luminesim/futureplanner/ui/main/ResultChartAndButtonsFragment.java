package com.luminesim.futureplanner.ui.main;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.luminesim.futureplanner.FactListActivity;
import com.luminesim.futureplanner.R;
import com.luminesim.futureplanner.Category;
import com.luminesim.futureplanner.db.EntityParameter;
import com.luminesim.futureplanner.db.EntityRepository;
import com.luminesim.futureplanner.simulation.EntityWithFundsSimulation;
import com.luminesim.futureplanner.simulation.SimpleIndividualIncomeSimulation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A placeholder fragment containing a simple view.
 */
public class ResultChartAndButtonsFragment extends Fragment {

    private static final String ARG_SECTION_NUMBER = "section_number";

    private PageViewModel pageViewModel;

    /**
     * Boolean extra.
     * If true, the simulation's facts have changed, warranting a re-run.
     */
    public static final String EXTRA_SIMULATION_FACTS_CHANGED = "com.luminesim.futureplanner.EXTRA_SIMULATION_FACTS_CHANGED";
    private EntityWithFundsSimulation mSimulation;
    private ExecutorService mRunner = Executors.newFixedThreadPool(1);
    private EntityRepository mRepo;

    private EditText mAmount;
    private Spinner mRuntime;
    private Button mStartButton;
    private LineChart mChart;

    private int mRuntimeInDays;

    public static ResultChartAndButtonsFragment newInstance(int index) {
        ResultChartAndButtonsFragment fragment = new ResultChartAndButtonsFragment();
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
    public void onResume() {
        super.onResume();
        mRepo = new EntityRepository(getContext());

        // Set up the UI.
        mAmount = getActivity().findViewById(R.id.initialFundsEditText);
        mRuntime = getActivity().findViewById(R.id.runDurationSpinner);
        mStartButton = getActivity().findViewById(R.id.startButton);
        mChart = getActivity().findViewById(R.id.chartArea);

        long entityUid = getActivity().getIntent().getLongExtra(getString(R.string.extra_entity_uid), 0l);
        mRepo.getEntity(entityUid, entityWithFacts -> {
            getActivity().runOnUiThread(() -> {
                String initialFunds = entityWithFacts.getParameter(SimpleIndividualIncomeSimulation.PARAMETER_INITIAL_FUNDS).get();
                mAmount.setText(initialFunds);
                Log.i("FUNDS", "initialFunds: " + initialFunds);
                mChart.setNoDataText("Tap start button");

                // Run the simulation.
                onStartButtonPressed(getView());
            });
        });
    }

    public void onStartButtonPressed(View view) {

        long entityUid = getActivity().getIntent().getLongExtra(getString(R.string.extra_entity_uid), 0l);


        mRepo.getEntity(entityUid, entityWithFacts -> {

            // Update the initial funds parameter.
            entityWithFacts
                    .getParameters()
                    .stream()
                    .filter(param -> param.getName().equals(SimpleIndividualIncomeSimulation.PARAMETER_INITIAL_FUNDS))
                    .forEach(param -> {
                        param.setValue(mAmount.getText().toString());
                        mRepo.updateParameter(entityUid, param);
                        Log.i("FuturePlanner", "Initial funds updated to " + param.getValue());
                    });

            // Determine the runtime.
            int MONTH = 30;
            int YEAR = 365;
            switch (mRuntime.getSelectedItemPosition()) {
                case 0:
                    mRuntimeInDays = 3 * MONTH;
                    break;
                case 1:
                    mRuntimeInDays = 6 * MONTH;
                    break;
                case 2:
                    mRuntimeInDays = 1 * YEAR;
                    break;
                case 3:
                    mRuntimeInDays = 2 * YEAR;
                    break;
                case 4:
                    mRuntimeInDays = 5 * YEAR;
                    break;
                default:
                    throw new IllegalStateException("Unknown runtime spinner value: " + mRuntime.getSelectedItemPosition());
            }

            // Run the simulation.
            runSimulation(entityUid);
        });
    }

    /**
     * Runs the simulation for the given entity.
     *
     * @param entityUid
     */
    private void runSimulation(long entityUid) {
        mSimulation = new SimpleIndividualIncomeSimulation(mRuntimeInDays, getContext(), entityUid);

        getActivity().runOnUiThread(() -> {
            // Invalidate the chart.
            mChart.setData(null);
            mChart.setNoDataText("Running...");

            // Disable controls.
            mAmount.setEnabled(false);
            mRuntime.setEnabled(false);
            mStartButton.setEnabled(false);
        });

        mRunner.submit(() -> {
            try {
                mSimulation.run();
                LineData data = getData(mSimulation);
                getActivity().runOnUiThread(() -> {
                    // Set up the chart and its  data.
                    mChart.setData(data);

                    // Fix the legend and description.
                    mChart.getDescription().setText("");

                    // Update the Y scale to be a sensible size, defaulting to zero as the ymin
                    // for a consistent scale. Also, remove the right axis as it adds no value.
                    mChart.getAxisLeft().setAxisMaximum(Math.max(100, data.getYMax() * 1.05f));
                    mChart.getAxisLeft().setAxisMinimum(Math.min(0, data.getYMin() - data.getYMin() * 0.05f));
                    mChart.getAxisRight().setAxisMaximum(0);
                    mChart.getAxisRight().setAxisMinimum(0);

                    // Update the X axis to use dates.
                    mChart.getXAxis().setValueFormatter(
                            new ValueFormatter() {
                                @Override
                                public String getAxisLabel(float value, AxisBase axis) {
                                    return LocalDate.ofEpochDay((long) value).toString();
                                }
                            }
                    );
//                    mChart.getXAxis().setLabelRotationAngle(-45);
                    mChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
                    mChart.getXAxis().setLabelRotationAngle(-30);

                    // Redraw.
                    mChart.invalidate();
//                    mRunner.submit(() -> {
//                        try {
//                            Thread.sleep(1000);
//
//
//                            // Redraw.
//                            getActivity().runOnUiThread(() ->
//                                    mChart.invalidate()
//                            );
//                        } catch (Throwable t) {
//                            Log.e("Error", "Problem sleeping while invalidating.", t);
//                        }
//                    });
                });
            } catch (Throwable t) {
                throw t;
            } finally {

                getActivity().runOnUiThread(() -> {
                    // Re-enable controls.
                    mAmount.setEnabled(true);
                    mRuntime.setEnabled(true);
                    mStartButton.setEnabled(true);
                    mChart.setNoDataText("Tap start button");
                });
            }
        });
    }

    private LineData getData(EntityWithFundsSimulation results) {
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


            container.findViewById(R.id.startButton).setOnClickListener(this::onStartButtonPressed);

//            ((EditText)container.findViewById(R.id.initialFundsEditText)).addTextChangedListener(new TextWatcher() {
//                @Override
//                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//
//                }
//
//                @Override
//                public void onTextChanged(CharSequence s, int start, int before, int count) {
//
//                    // Invalidate the chart.
//                    mChart.setData(null);
//                    mChart.setNoDataText("Tap start to re-run");
//                }
//
//                @Override
//                public void afterTextChanged(Editable s) {
//
//                }
//            });


            // Load ads.
            AdView mAdView = container.findViewById(R.id.adView);
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
        });
        return root;
    }
}