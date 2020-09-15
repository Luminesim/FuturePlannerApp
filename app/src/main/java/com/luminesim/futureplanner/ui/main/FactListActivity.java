package com.luminesim.futureplanner.ui.main;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.luminesim.futureplanner.R;
import com.luminesim.futureplanner.db.EntityFact;
import com.luminesim.futureplanner.db.EntityRepository;
import com.luminesim.futureplanner.monad.MonadDatabase;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.luminesim.futureplanner.purchases.FeatureManager;
import com.luminesim.futureplanner.purchases.FeatureSet;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Comparator;

public class FactListActivity extends AppCompatActivity {

    public static final String LIST_SELECTION = "LIST_SELECTION";
    public static final String LIST_TITLE = "LIST_TITLE";
    private static final int RC_ADD = 1;
    private static final long UID_DNE = 0l;
    private int mCategoryTextId;
    private MonadDatabase mData;
    private LinearLayout mList;
    private FeatureManager mFeatures;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fact_list);
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
                    ((CoordinatorLayout.LayoutParams)fab.getLayoutParams()).bottomMargin = (int)getResources().getDimension(R.dimen.fab_bottom_padding_with_ads);
                } else {
                    mAdView.setVisibility(View.GONE);
                    ((CoordinatorLayout.LayoutParams)fab.getLayoutParams()).bottomMargin = (int)getResources().getDimension(R.dimen.fab_bottom_padding_without_ads);
                }
            }

            @Override
            public void onErrorLoadingFeatures() {
                if (mAdView == null)
                    return;
                FeatureSet features = mFeatures.getPurchasedFeatures(false);
                if (features.isAdvertisingEnabled()) {
                    mAdView.setVisibility(View.VISIBLE);
                    ((CoordinatorLayout.LayoutParams)fab.getLayoutParams()).bottomMargin = (int)getResources().getDimension(R.dimen.fab_bottom_padding_with_ads);
                } else {
                    mAdView.setVisibility(View.GONE);
                    ((CoordinatorLayout.LayoutParams)fab.getLayoutParams()).bottomMargin = (int)getResources().getDimension(R.dimen.fab_bottom_padding_without_ads);
                }
            }
        });
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
                            .sorted(Comparator.comparing(ef -> ef.getFact().getName()))
                            .forEach(ef -> {
                                EntityFact fact = ef.getFact();
                                View toAdd = getLayoutInflater().inflate(R.layout.view_selectable, null);

                                // Build up the string to display.
                                StringBuilder text = new StringBuilder();
                                text.append(fact.getName()).append(": ");
                                ef.getDetails().forEach(detail -> text.append(mData.getFormattedStringFromJson(detail.getMonadJson())).append(" "));
                                ((TextView) toAdd.findViewById(R.id.chip)).setText(text.toString().trim());

                                // If the fact is clicked, update it.
                                toAdd.findViewById(R.id.chip).setOnClickListener(l -> {
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
    }
}