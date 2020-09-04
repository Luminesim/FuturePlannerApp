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

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.SkuDetails;
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
import com.luminesim.futureplanner.purchases.FeatureManager;
import com.luminesim.futureplanner.purchases.FeatureSet;
import com.luminesim.futureplanner.simulation.EntityWithFundsSimulation;
import com.luminesim.futureplanner.simulation.SimpleIndividualIncomeSimulation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link StoreFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class StoreFragment extends Fragment {

    private static final String ARG_SECTION_NUMBER = "section_number";

    private PageViewModel pageViewModel;

    private FeatureManager mFeatureManager;
    private View mList;
    public static final int PAGE_INDEX = 2;

    public static StoreFragment newInstance() {
        StoreFragment fragment = new StoreFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pageViewModel = ViewModelProviders.of(this).get(PageViewModel.class);
        pageViewModel.setIndex(PAGE_INDEX);
    }

    private void showConnectionError(BillingResult billingResult) {
    }

    private void showStoreItems(Map<SkuDetails, Boolean> skuDetails) {

        ((LinearLayout) mList.findViewById(R.id.layoutStoreCards)).removeAllViews();

        // Add all items.
        skuDetails.entrySet().forEach(kvp -> {

            SkuDetails item = kvp.getKey();
            boolean isPurchased = kvp.getValue();

            View toAdd = getLayoutInflater().inflate(R.layout.view_store_card, null);
            ((TextView) toAdd.findViewById(R.id.labelStoreItemDescription)).setText(item.getDescription());

            String skuTitleAppNameRegex = "(?> \\(.+?\\))$";
            Pattern p = Pattern.compile(skuTitleAppNameRegex, Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(item.getTitle());
            String titleWithoutAppName = m.replaceAll("");

            ((TextView) toAdd.findViewById(R.id.labelStoreItemName)).setText(titleWithoutAppName);


            // If the SKU is already purchased, disable the buy button.
            Button purchaseButton = ((Button) toAdd.findViewById(R.id.buttonPurchase));
            if (isPurchased) {
                purchaseButton.setText(R.string.button_owned);
                purchaseButton.setEnabled(false);
            } else {
                purchaseButton.setText(item.getPriceCurrencyCode() + item.getPrice());
                purchaseButton.setOnClickListener(view -> {
                    mFeatureManager.launchBillingFlow(getActivity(), item);
                });
            }
            ((LinearLayout) mList.findViewById(R.id.layoutStoreCards)).addView(toAdd);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        delayTime.set(1000);
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mList = inflater.inflate(R.layout.fragment_store, container, false);
        pageViewModel.getText().observe(getViewLifecycleOwner(), s -> {

//            container.findViewById(R.id.buttonIncome).setOnClickListener(view -> {
//                Intent intent = new Intent(getContext(), FactListActivity.class);
//                intent.putExtra(FactListActivity.LIST_TITLE, R.string.button_income);
//                intent.putExtra(FactListActivity.LIST_SELECTION, Category.Income);
//                intent.putExtra(getString(R.string.extra_entity_uid), getActivity().getIntent().getLongExtra(getString(R.string.extra_entity_uid), 0l));
//                startActivity(intent);
//            });
        });

        mFeatureManager = new FeatureManager(getContext());
        Log.i("FeatureManager", "Created");
        mFeatureManager.listen(new FeatureManager.FeatureManagerListener() {
            @Override
            public void onProductListReady() {
                Log.i("FeatureManager", "Product list ready.");
                showStoreItems(mFeatureManager.getProductDetailsAndOwnership());
            }

            @Override
            public void onConnectionOpened(BillingResult newStatus) {
                Log.i("FeatureManager", "Connection open");

            }

            @Override
            public void onConnectionClosed() {
                Log.i("FeatureManager", "Connection closed. Trying to reopen in " + delayTime.get() + "ms.");
                retryThread = new Thread(() -> {
                    try {
                        Thread.sleep(delayTime.get());
                        delayTime.set(delayTime.get() * 2);
                        mFeatureManager.tryConnectBillingClient();
                    } catch (Throwable t) {

                    }
                });
                retryThread.start();
            }

            @Override
            public void onFeaturesUpdated() {
                Log.i("FeatureManager", "Features updated");
                showStoreItems(mFeatureManager.getProductDetailsAndOwnership());
            }
        });

        return mList;
    }

    private Thread retryThread;
    private AtomicLong delayTime = new AtomicLong(1000);
}