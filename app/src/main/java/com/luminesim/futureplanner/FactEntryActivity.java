package com.luminesim.futureplanner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.luminesim.futureplanner.db.EntityFact;
import com.luminesim.futureplanner.db.EntityFactDetail;
import com.luminesim.futureplanner.db.EntityFactWithDetails;
import com.luminesim.futureplanner.db.EntityRepository;
import com.luminesim.futureplanner.monad.MonadData;
import com.luminesim.futureplanner.monad.MonadSelectionView;
import com.luminesim.futureplanner.monad.types.OneOffAmount;
import com.luminesim.futureplanner.ui.main.ResultChartAndButtonsFragment;

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
    private List<MonadData> data = new ArrayList<>();
    private EntityRepository mEntities;
    private long mEntityUid;
    private long mFactUid;
    private Category mCategory;

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
        EditText editText = findViewById(R.id.commandEditText);
        mAdapter = new MonadSelectionView(
                this,
                mCategory,
                (formattedString, monadId, parameters) -> {
                    editText.append(" " + formattedString);
                    try {
                        setSaveButtonState();

                        // Add to the list of data.
                        MonadData next = new MonadData(monadId, parameters);
                        data.add(next);
                    } catch (Throwable t) {
                        throw new RuntimeException(t);
                    }
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
    }

    /**
     * Sets the save button's state based on the current text in the editable field
     * and the current selection.
     */
    private void setSaveButtonState(Editable name) {

        // There needs to be a non-whitespace name.
        boolean isEnabled = (name.toString().trim().length() > 0 && !name.toString().trim().equals(""))
                // And the selection must provide an output type.
                && mAdapter.getCurrentSelectionOutputType().isPresent();

        // If this is income or expenses, there can only be two types of numbers coming out: rates or one-off events.
        if (isEnabled) {
            if (mCategory == Category.Income || mCategory == Category.Expenses) {
                Class<?> outType = mAdapter.getCurrentSelectionOutputType().get();
                isEnabled &= Rate.class.isAssignableFrom(outType) || OneOffAmount.class.isAssignableFrom(outType);
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
                runOnUiThread(() -> ((EditText)findViewById(R.id.commandEditText)).setText(""));
                data.getDetails().forEach(detail -> {
                    runOnUiThread(() -> mAdapter.triggerCallbackAndUpdateMonadList(detail));
                });
            });
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
            for (int i = 0; i < data.size(); i += 1) {
                EntityFactDetail detail = EntityFactDetail
                        .builder()
                        .stepNumber(i)
                        .monadJson(data.get(i).toJson())
                        .build();
                details.add(detail);
            }

            // Submit the update.
            mEntities.updateFact(mEntityUid, fact, details, () -> {
                // All done.
                Intent out = new Intent();
                EditText editText = findViewById(R.id.commandEditText);
                out.putExtra(EXTRA_FORMATTED_TEXT, editText.getText().toString());
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
        EditText editText = findViewById(R.id.commandEditText);
        editText.setText("");
        data.clear();
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
}