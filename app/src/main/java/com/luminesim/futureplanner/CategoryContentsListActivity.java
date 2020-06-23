package com.luminesim.futureplanner;

import android.content.Intent;
import android.os.Bundle;

import com.luminesim.futureplanner.db.EntityFact;
import com.luminesim.futureplanner.db.EntityRepository;
import com.luminesim.futureplanner.monad.MonadDatabase;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class CategoryContentsListActivity extends AppCompatActivity {

    public static final String LIST_SELECTION = "LIST_SELECTION";
    public static final String LIST_TITLE = "LIST_TITLE";
    private static final int RC_ADD = 1;
    private static final long UID_DNE = 0l;
    private int mCategoryTextId;
    private MonadDatabase mData;
    private LinearLayout mList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_contents_list);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mData = MonadDatabase.getDatabase(this);

        mCategoryTextId = getIntent().getIntExtra(LIST_TITLE, -1);
        if (mCategoryTextId == -1) {
            throw new IllegalStateException("Did not receive a title.");
        }
        getSupportActionBar().setTitle(mCategoryTextId);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            updateFact(UID_DNE);
        });
        mList = findViewById(R.id.categoryContentItemList);

        // Inflate a number of text boxes with one for each entity.
        updateList();
    }

    private void updateList() {
        mList.removeAllViews();
        EntityRepository repo = new EntityRepository(getApplicationContext());
        repo.getEntity(
                getIntent().getLongExtra(getString(R.string.extra_entity_uid), 0),
                ewf -> {
                    // Show facts for this category.
                    ewf.getFacts()
                            .stream()
                            .filter(ef -> ef.getFact().getCategory().name().equals(getString(mCategoryTextId)))
                            .forEach(ef -> {
                                EntityFact fact = ef.getFact();
                                View toAdd = getLayoutInflater().inflate(R.layout.monad_selection_layout, null);

                                // Build up the string to display.
                                StringBuilder text = new StringBuilder();
                                text.append(fact.getName()).append(": ");
                                ef.getDetails().forEach(detail -> text.append(mData.getFormattedStringFromJson(detail.getMonadJson())).append(" "));
                                ((TextView) toAdd.findViewById(R.id.label)).setText(text.toString().trim());

                                // If the fact is clicked, update it.
                                toAdd.setOnClickListener(l -> {
                                    updateFact(fact.getUid());
                                });
                                runOnUiThread(() -> mList.addView(toAdd));
                            });


                });
    }

    private void updateFact(long factUid) {
        Intent intent = new Intent(this, FactEntryActivity.class);
        intent.putExtra(getString(R.string.extra_entity_uid), getIntent().getLongExtra(getString(R.string.extra_entity_uid), 0l));
        intent.putExtra(getString(R.string.extra_fact_uid), getIntent().getLongExtra(getString(R.string.extra_fact_uid), factUid));
        intent.putExtra(FactEntryActivity.EXTRA_DATA_CATEGORY, getString(mCategoryTextId));
        startActivityForResult(intent, RC_ADD);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Update the list if needed.
        runOnUiThread(this::updateList);
        if (resultCode == RESULT_OK) {
//            runOnUiThread(this::updateList);
        }

//        // create a new view
//        LinearLayout view = findViewById(R.id.categoryContentItemList);
//        View toAdd = getLayoutInflater().inflate(R.layout.monad_selection_layout, null);
//        ((TextView) toAdd.findViewById(R.id.label)).setText(String.format(
//                "%s: %s",
//                data.getStringExtra(FactEntryActivity.EXTRA_NAME),
//                data.getStringExtra(FactEntryActivity.EXTRA_FORMATTED_TEXT)
//        ));
//        view.addView(toAdd);
//        if (requestCode == RC_ADD) {
//            Log.i("TEST", "Result: " + data.getStringExtra("DATA"));
//        } else {
//            Log.i("TEST", "Unknown request code: " + requestCode);
//        }
    }
}