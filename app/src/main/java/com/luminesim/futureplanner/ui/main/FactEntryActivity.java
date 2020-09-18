package com.luminesim.futureplanner.ui.main;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.luminesim.futureplanner.Category;
import com.luminesim.futureplanner.R;
import com.luminesim.futureplanner.db.EntityFact;
import com.luminesim.futureplanner.db.EntityFactDetail;
import com.luminesim.futureplanner.db.EntityFactWithDetails;
import com.luminesim.futureplanner.db.EntityRepository;
import com.luminesim.futureplanner.monad.MonadData;
import com.luminesim.futureplanner.monad.MonadSelectionView;
import com.luminesim.futureplanner.monad.types.OneOffAmount;
import com.luminesim.futureplanner.purchases.FeatureManager;
import com.luminesim.futureplanner.purchases.FeatureSet;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import ca.anthrodynamics.indes.lang.Rate;

public class FactEntryActivity extends AppCompatActivity {
    public static final String EXTRA_NAME = "com.exmaple.myfirstapp.NAME";
    public static final String EXTRA_FORMATTED_TEXT = "com.exmaple.myfirstapp.FORMATTED_TEXT";
    public static final String EXTRA_DATA_CATEGORY = "com.extra.myfirstapp.EXTRA_DATA_CATEGORY";
    private static final long NO_ID = 0l;
    public static final int RESULT_OK_FACT_DELETED = 100;

    private RecyclerView recyclerView;
    private MonadSelectionView mAdapter;
    private RecyclerView.LayoutManager layoutManager;
    private List<MonadData> mData = new ArrayList<>();
    private EntityRepository mEntities;
    private long mEntityUid;
    private long mFactUid;
    private Category mCategory;
    private FeatureManager mFeatures;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fact_entry);

        // Data setup.
        recyclerView = (RecyclerView) findViewById(R.id.predictive_option_list);
        mEntities = new EntityRepository(getApplicationContext());
        mCategory = Category.valueOf(getIntent().getStringExtra(EXTRA_DATA_CATEGORY));

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);

        // use a linear layout manager
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        // specify an adapter (see also next example)
        ChipGroup preview = findViewById(R.id.previewAmount);
        TextView label = findViewById(R.id.labelAmount);
        mAdapter = new MonadSelectionView(
                this,
                mCategory,
                (formattedString, hint, monadId, parameters) -> {
                    View v = getLayoutInflater().inflate(R.layout.view_fact_chip, null);
                    Chip next = ((Chip) v.findViewById(R.id.chip));

                    // This allows us to take a formatted chip and put it into the desired list view
                    ((ConstraintLayout) v.findViewById(R.id.layout)).removeView(next);

                    // Update the chip's text now that its in the right spot
                    next.setText(formattedString);

                    // Ensure everything is visible again.
                    label.setVisibility(View.VISIBLE);
                    preview.setVisibility(View.VISIBLE);

                    // Add the chip to the view, remembering its position
                    preview.addView(next);
                    final int Index = preview.getChildCount() - 1;

                    // Allow the chip to be edited.
                    next.setOnClickListener(view -> {
                        for (int i = 0; i < preview.getChildCount(); i += 1) {
                            ((Chip) preview.getChildAt(i)).setChipBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.canvasColor)));
                        }

                        if (!next.isChecked()) {
                            mAdapter.cancelEdit();
                        } else {
                            next.setChipBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.selectionColor)));
                            mAdapter.editSelection(Index, getSupportFragmentManager(), () -> {
                                int count = preview.getChildCount();
                                preview.removeViews(Index, preview.getChildCount() - (Index));
                                mData = mData.subList(0, Index);
                                for (int i = 0; i < preview.getChildCount(); i += 1) {
                                    ((Chip) preview.getChildAt(i)).setChipBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.canvasColor)));
                                }

                            });
                        }
                    });


                    try {
                        setSaveButtonState();

                        // Add to the list of data.
                        MonadData nextData = new MonadData(monadId, parameters);
                        mData.add(nextData);
                    } catch (Throwable t) {
                        throw new RuntimeException(t);
                    }
                },
                (position) -> {

                    // Dump everything
                    mData = mData.subList(0, position);
                });
        recyclerView.setAdapter(mAdapter);

        // Get the entity and fact.
        mEntityUid = getIntent().getLongExtra(getString(R.string.extra_entity_uid), NO_ID);
        mFactUid = getIntent().getLongExtra(getString(R.string.extra_fact_uid), NO_ID);

        // Ensure that the name field causes the save button to enable / disable.
        ((EditText) findViewById(R.id.textFactName)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Nothing to do.
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                // When the text changes, we may need to toggle buttons.
                setSaveButtonState(s);
            }
        });

        // Set whether or not save should be enabled.
        setSaveButtonState();

        // Load ads.
        AdView mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
        mAdView.setVisibility(View.GONE);

        mFeatures = new FeatureManager(this);
        mFeatures.listen(new FeatureManager.FeatureManagerListener() {
            @Override
            public void onProductListReady() {
            }

            @Override
            public void onFeaturesUpdated() {
                FeatureSet features = mFeatures.getPurchasedFeatures(false);
                if (features.isAdvertisingEnabled()) {
                    mAdView.setVisibility(View.VISIBLE);
                } else {
                    mAdView.setVisibility(View.GONE);
                }
            }

            @Override
            public void onErrorLoadingFeatures() {
                if (mAdView == null)
                    return;
                FeatureSet features = mFeatures.getPurchasedFeatures(false);
                if (features.isAdvertisingEnabled()) {
                    mAdView.setVisibility(View.VISIBLE);
                } else {
                    mAdView.setVisibility(View.GONE);
                }
            }
        });
    }

    /**
     * Sets the save button's state based on the current text in the editable field
     * and the current selection.
     */
    private void setSaveButtonState(Editable name) {

        // There needs to be a non-whitespace name.
        boolean isEnabled = (name.toString().trim().length() > 0 && !name.toString().trim().equals(""));

        // If this is income or expenses, there can only be two types of numbers coming out: rates or one-off events.
        if (isEnabled) {
            if (mCategory == Category.Income || mCategory == Category.Expenses) {
                isEnabled &= (mAdapter.doesSelectionProduceType(Rate.class)
                        || mAdapter.doesSelectionProduceType(OneOffAmount.class));
            } else {
                throw new Error("Unhandled fact category: " + mCategory);
            }
        }

        // Toggle enabled based on the above criteria.
        findViewById(R.id.buttonSave).setEnabled(isEnabled);
    }

    /**
     * Sets the save button's state based on the current text in the name field
     * and the current selection.
     */
    private void setSaveButtonState() {
        setSaveButtonState(((EditText) findViewById(R.id.textFactName)).getText());
    }

    @Override
    protected void onResume() {
        super.onResume();


        // If the fact exists, enter the data into the form.
        if (mFactUid != NO_ID) {
            mEntities.getFact(mFactUid, data -> {
                runOnUiThread(() -> ((EditText) findViewById(R.id.textFactName)).setText(data.getFact().getName()));

                // Enter the details one by one.
                runOnUiThread(() -> {
                    ChipGroup preview = findViewById(R.id.previewAmount);
                    TextView label = findViewById(R.id.labelAmount);
                    preview.removeAllViews();
                    preview.setVisibility(View.GONE);
                    label.setVisibility(View.GONE);
                });
                if (data.getDetails() != null && data.getDetails().size() > 0) {
                    runOnUiThread(() -> {
                        ChipGroup preview = findViewById(R.id.previewAmount);
                        TextView label = findViewById(R.id.labelAmount);
                        preview.removeAllViews();
                        preview.setVisibility(View.VISIBLE);
                        label.setVisibility(View.VISIBLE);
                    });
                }
                data.getDetails().forEach(detail -> {
                    runOnUiThread(() -> mAdapter.triggerCallbackAndUpdateMonadList(detail));
                });
            });
        }
        else {


            ChipGroup previewAmount = findViewById(R.id.previewAmount);
            TextView labelAmount = findViewById(R.id.labelAmount);
            if (previewAmount.getChildCount() == 0) {
                previewAmount.setVisibility(View.GONE);
                labelAmount.setVisibility(View.GONE);
            }
        }

        // Ensure the save button is properly enabled / disabled.
        setSaveButtonState();
    }

    /**
     * Called when the user taps the save button.
     */
    public void saveSelection(View view) {

        // Disable the save and cancel button while things save.
        findViewById(R.id.buttonSave).setEnabled(false);
        findViewById(R.id.buttonClear).setEnabled(false);

        // Do the update.
        String name = ((EditText) findViewById(R.id.textFactName)).getText().toString();
        mEntities.getEntity(mEntityUid, current -> {

            // Find the fact we're updating. If it doesn't exist, create one.
            EntityFact fact = current
                    .getFacts()
                    .stream()
                    .filter(f -> {
                        Log.i(getClass().getName(), "Next fact is " + f.getFact());
                        return f.getFact().getUid() == mFactUid;
                    })
                    .findFirst()
                    .orElseGet(() -> new EntityFactWithDetails(
                            EntityFact.builder().category(mCategory).entityUid(mEntityUid).name(name).build(),
                            new ArrayList<>())
                    )
                    .getFact();

            // Since we might need to update the fact's name and we may need to get an ID, do so.
            fact.setName(name);

            // Collect all details.
            List<EntityFactDetail> details = new LinkedList<>();
            for (int i = 0; i < mData.size(); i += 1) {
                EntityFactDetail detail = EntityFactDetail
                        .builder()
                        .stepNumber(i)
                        .monadJson(mData.get(i).toJson())
                        .build();
                details.add(detail);
            }

            // Submit the update.
            mEntities.updateFact(mEntityUid, fact, details, () -> {
                // All done.
                Intent out = new Intent();
                ChipGroup result = findViewById(R.id.previewAmount);
                String text = "";
                for (int i = 0; i < result.getChildCount(); i += 1) {
                    text += result.getChildAt(i).toString() + " ";
                }
                text.trim();
                out.putExtra(EXTRA_FORMATTED_TEXT, text);
                out.putExtra(ResultChartAndButtonsFragment.EXTRA_SIMULATION_FACTS_CHANGED, true);
                out.putExtra(EXTRA_NAME, name);
                setResult(RESULT_OK, out);
                finish();
            });
        });
    }

    /**
     * Called when the user taps the clear button
     */
    public void clearSelection(@NonNull View view) {
        ChipGroup editArea = findViewById(R.id.previewAmount);
        editArea.removeAllViews();
        TextView label = findViewById(R.id.labelAmount);
        editArea.setVisibility(View.GONE);
        label.setVisibility(View.GONE);
        mData.clear();
        mAdapter.restartSelection();
        setSaveButtonState();
    }

    /**
     * Called when the user taps the delete button
     */
    public void deleteSelection(@NonNull View view) {

        // Disable the save and cancel button while things delete.
        findViewById(R.id.buttonSave).setEnabled(false);
        findViewById(R.id.buttonClear).setEnabled(false);

        // Delete the fact (if it already exists).
        boolean simulationInputsChanged = false;
        if (mFactUid != NO_ID) {
            mEntities.deleteFact(mFactUid);
            simulationInputsChanged = true;
        }

        // All done.
        Intent out = new Intent();
        out.putExtra(ResultChartAndButtonsFragment.EXTRA_SIMULATION_FACTS_CHANGED, simulationInputsChanged);
        setResult(RESULT_OK_FACT_DELETED, out);
        finish();
    }

    public void showChipgroupHint(@NonNull View view) {
        new AlertDialog.Builder(this)
                .setMessage(R.string.hint_autocomplete)
                .setNeutralButton(R.string.button_ok, (x, y) -> {
                })
                .show();
    }
}