package com.luminesim.futureplanner.ui.main;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

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
import com.luminesim.futureplanner.R;
import com.luminesim.futureplanner.Category;
import com.luminesim.futureplanner.db.EntityRepository;
import com.luminesim.futureplanner.purchases.CanNavigateToStore;
import com.luminesim.futureplanner.purchases.FeatureManager;
import com.luminesim.futureplanner.purchases.FeatureSet;
import com.luminesim.futureplanner.simulation.EntityWithFundsSimulation;
import com.luminesim.futureplanner.simulation.SimpleIndividualIncomeSimulation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A placeholder fragment containing a simple view.
 */
public class ResultChartAndButtonsFragment extends Fragment {

    private static final String ARG_SECTION_NUMBER = "section_number";

    private long lastAutoRunTimeMs = 0l;

    private PageViewModel pageViewModel;
    private AdView mAdView;
    public static final int PAGE_INDEX = 1;

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
    private FeatureManager mFeatures;

    public static ResultChartAndButtonsFragment newInstance() {
        ResultChartAndButtonsFragment fragment = new ResultChartAndButtonsFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pageViewModel = ViewModelProviders.of(this).get(PageViewModel.class);
        pageViewModel.setIndex(PAGE_INDEX);


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
        if (System.currentTimeMillis() - lastAutoRunTimeMs > 100) {
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
            lastAutoRunTimeMs = System.currentTimeMillis();
        }
    }

    public void onStartButtonPressed(View view) {

        // Ensure we have the latest features.
        mLatestFeatures = mFeatures.getPurchasedFeatures(true);

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
            int MONTH = 31;
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
                    if (!mLatestFeatures.isTwoYearRuntimeEnabled()) {
                        getActivity().runOnUiThread(() -> new AlertDialog
                                .Builder(getActivity())
                                .setTitle(R.string.title_error)
                                .setMessage(R.string.runtime_feature_not_purchased)
                                .setNegativeButton(R.string.maybe_later, (dialog, which) -> {
                                })
                                .setPositiveButton(R.string.go_to_store, (dialog, which) -> ((CanNavigateToStore) getActivity()).navigateToStore(FeatureSet.builder().isTwoYearRuntimeEnabled(true).build()))
                                .create()
                                .show());
                        return;
                    }
                    mRuntimeInDays = 2 * YEAR;
                    break;
                case 4:
                    if (!mLatestFeatures.isFiveYearRuntimeEnabled()) {
                        getActivity().runOnUiThread(() -> new AlertDialog
                                .Builder(getActivity())
                                .setTitle(R.string.title_error)
                                .setMessage(R.string.runtime_feature_not_purchased)
                                .setNegativeButton(R.string.maybe_later, (dialog, which) -> {
                                })
                                .setPositiveButton(R.string.go_to_store, (dialog, which) -> ((CanNavigateToStore) getActivity()).navigateToStore(FeatureSet.builder().isFiveYearRuntimeEnabled(true).build()))
                                .create()
                                .show());
                        return;
                    }
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

                // NOTE: For some reason the rendering engine struggles with rotated / large
                // axis labels, so we need to draw TWICE for things to display correctly.
                for (int i = 0; i < 2; i += 1) {
                    Optional<LineData> result = getData(mSimulation);

                    getActivity().runOnUiThread(() -> {

                        if (!result.isPresent()) {
                            mChart.setData(null);
                            mChart.setNoDataText(getString(R.string.error_infinity_or_nan_result_short_message));
                            new AlertDialog
                                    .Builder(getActivity())
                                    .setTitle(R.string.title_error)
                                    .setNeutralButton(R.string.button_ok, (x, y) -> {
                                    })
                                    .setMessage(R.string.error_infinity_or_nan_result_long_message)
                                    .show();
                            mChart.invalidate();
                        } else {
                            LineData data = result.get();
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
                            mChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
                            mChart.getXAxis().setLabelRotationAngle(-30);

                            // Redraw.
                            mChart.postInvalidate();
                            mChart.setNoDataText("Tap start button");
                        }
                    });
                }
            } catch (Throwable t) {
                throw t;
            } finally {

                getActivity().runOnUiThread(() -> {
                    // Re-enable controls.
                    mAmount.setEnabled(true);
                    mRuntime.setEnabled(true);
                    mStartButton.setEnabled(true);
                });
            }
        });
    }

    /**
     * Gets the results of the simulation. If any amount hits infinity or results in NAN, an empty
     * value is returned.
     *
     * @param results
     * @return
     */
    private Optional<LineData> getData(EntityWithFundsSimulation results) {

        // Sanity check: if funds reaches infinity or nan, it can't be displayed.
        if (results.getFundsDataset().values().stream().anyMatch(value -> value.isInfinite() || value.isNaN() || Float.isInfinite(value.floatValue()) || Float.isNaN(value.floatValue()))) {
            return Optional.empty();
        }

        // Get the data for the dataset.
        List<Entry> entries = new ArrayList<>(results.getFundsDataset().size());
        results.getFundsDataset().forEach((date, funds) -> entries.add(new Entry((float) date.toEpochDay(), funds.floatValue())));

        // Make it pretty.
        LineDataSet set1 = new LineDataSet(entries, getString(R.string.chart_legend_funds));
        set1.setDrawFilled(true);
        set1.setAxisDependency(YAxis.AxisDependency.LEFT);
        set1.setColor(ColorTemplate.getHoloBlue());
        set1.setFillColor(ColorTemplate.getHoloBlue());
        ;
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
        return Optional.of(data);
    }

    private FeatureSet mLatestFeatures = FeatureSet.noFeatures();

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);


        if (mFeatures == null) {
            mFeatures = new FeatureManager(getContext());
            mFeatures.listen(new FeatureManager.FeatureManagerListener() {
                @Override
                public void onProductListReady() {
                }

                @Override
                public void onFeaturesUpdated() {
                    mLatestFeatures = mFeatures.getPurchasedFeatures(false);
                    if (mLatestFeatures.isAdvertisingEnabled()) {
                        mAdView.setVisibility(View.VISIBLE);
                    } else {
                        mAdView.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onErrorLoadingFeatures() {
                    if (mAdView == null)
                        return;
                    mLatestFeatures = mFeatures.getPurchasedFeatures(false);
                    if (mLatestFeatures.isAdvertisingEnabled()) {
                        mAdView.setVisibility(View.VISIBLE);
                    } else {
                        mAdView.setVisibility(View.GONE);
                    }
                }
            });
        }
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

            // Load ads.
            mAdView = container.findViewById(R.id.adView);
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
            mAdView.setVisibility(View.GONE);
        });
        return root;
    }
}