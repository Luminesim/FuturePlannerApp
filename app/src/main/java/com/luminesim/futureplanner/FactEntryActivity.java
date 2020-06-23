package com.luminesim.futureplanner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.luminesim.futureplanner.db.EntityFact;
import com.luminesim.futureplanner.db.EntityFactDetail;
import com.luminesim.futureplanner.db.EntityFactWithDetails;
import com.luminesim.futureplanner.db.EntityRepository;
import com.luminesim.futureplanner.monad.MonadData;
import com.luminesim.futureplanner.monad.UserFacingMonadList;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import ca.anthrodynamics.indes.lang.Rate;

public class FactEntryActivity extends AppCompatActivity {
    public static final String EXTRA_NAME = "com.exmaple.myfirstapp.NAME";
    public static final String EXTRA_FORMATTED_TEXT = "com.exmaple.myfirstapp.FORMATTED_TEXT";
    public static final String EXTRA_DATA_CATEGORY = "com.extra.myfirstapp.EXTRA_DATA_CATEGORY";
    private static final long NO_ID = 0l;

    private RecyclerView recyclerView;
    private UserFacingMonadList mAdapter;
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


        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);

        // use a linear layout manager
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        // specify an adapter (see also next example)
        EditText editText = findViewById(R.id.commandEditText);
        mAdapter = new UserFacingMonadList(
                this,
                (formattedString, monadId, parameters) -> {
                    editText.append(" " + formattedString);
                    try {

                        // If the monad produces the correct type, enable the save button.
                        // TODO FIXME: WORKS ONLY FOR INCOME, EXPENSES.
                        if (mCategory == Category.Income || mCategory == Category.Expenses) {
                            if (Rate.class.isAssignableFrom(mAdapter.getOutputType(monadId))) {
                                findViewById(R.id.buttonSave).setEnabled(true);
                            }
                        }
                        else {
                            throw new Error("Unhandled category for save filter: " + mCategory);
                        }

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
        mCategory = Category.valueOf(getIntent().getStringExtra(EXTRA_DATA_CATEGORY));
    }

    @Override
    protected void onResume() {
        super.onResume();

        // If the fact exists, enter the data into the form.
        if (mFactUid != NO_ID) {
            mEntities.getFact(mFactUid, data -> {
                runOnUiThread(() -> ((EditText) findViewById(R.id.textFactName)).setText(data.getFact().getName()));

                // Enter the details one by one.
                data.getDetails().forEach(detail -> {
                    runOnUiThread(() -> mAdapter.triggerCallbackAndUpdateMonadList(detail));
                });
            });
        }
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
                out.putExtra(EXTRA_NAME, name);
                setResult(RESULT_OK, out);
                finish();
            });
        });
    }

    /**
     * Called when the user taps the clear button
     */
    public void clearSelection(View view) {
        EditText editText = (EditText) findViewById(R.id.commandEditText);
        editText.setText("");
        data.clear();
        findViewById(R.id.buttonSave).setEnabled(false);
        mAdapter.restartSelection();
    }
}