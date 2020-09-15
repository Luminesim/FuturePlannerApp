package com.luminesim.futureplanner.purchases;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.NonNull;

/**
 * Tracks which features are enabled, disabled, and/or purchased.
 * Lisenter lifecycle:
 * <ol>
 * <li>{@link FeatureManagerListener#onConnectionOpened(BillingResult)} when the connection opens</li>
 * <li>{@link FeatureManagerListener#onProductListReady()} when the list is ready and if a listener connections while the list is ready</li>
 * <li>{@link FeatureManagerListener#onFeaturesUpdated()} (List)} when the list is ready and if a listener connects while the list is ready</li>
 * </ol>
 * If {@link FeatureManagerListener#onConnectionClosed()} is called the cycle resets.
 */
public class FeatureManager implements PurchasesUpdatedListener {

    private BillingClient billingClient;

    private static String UNLOCK_APP_NO_ADDS = "unlock.app.remove_ads";
    private static String UNLOCK_APP_UP_TO_FIVE_YEAR_RUNTIME = "unlock.app.up_to_five_year_runtime";

//    private static String UNLOCK_APP_NO_ADS_UP_TO_FIVE_YEAR_RUNTIME = "unlock.app.remove_ads_up_to_five_year_runtime";
//    private static String UNLOCK_MODEL_SIMPLE_FREEMIUM_BASS_DIFFUSION = "unlock.model.simple_freemium_bass_diffusion";

    private Map<String, FeatureSet> activeFeatureSets = new HashMap<>();
    private Map<String, SkuDetails> productList = new HashMap<>();
    private Map<String, Purchase> purchases = new HashMap<>();
    private Set<FeatureSet> purchasedFeatures = new HashSet<>();

    @Getter
    private boolean isConnected = false;

    @Getter
    private Set<FeatureManagerListener> listeners = new HashSet<>();

    /**
     * WARNING: THIS METHOD WILL CONSUME ALL PURCHASES THE USER HAS MADE.
     * THIS SHOULD ONLY EVER BE EXECUTED IN TEST.
     */
//    public void consumeAllPurchases() {
//            purchases.values().forEach(purchase -> {
//            ConsumeParams consumeParams =
//                    ConsumeParams.newBuilder()
//                            .setPurchaseToken(purchase.getPurchaseToken())
//                            .build();
//            billingClient.consumeAsync(consumeParams, (billingResult, s) -> {
//                Log.i("FeatureManager CONSUME ALL", billingResult.toString());
//            });
//        });
//    }

    public FeatureManager(@NonNull Context context) {

        initializeActiveFeatureSets();

        // Create the billing client
        billingClient = BillingClient.newBuilder(context)
                .enablePendingPurchases()
                .setListener(this)
                .build();

        tryConnectBillingClient();
        updateProductsPurchasesAndFeatureLists();
    }

    private void updateProductsPurchasesAndFeatureLists() {

        updatePurchases();
        updateProductList();
        updateFeatureSet();
    }

    private boolean updateFeatureSet() {
        if (!isConnected())
            return false;

        listeners.forEach(listener -> listener.onFeaturesUpdated());
        return true;
    }

    private void initializeActiveFeatureSets() {
        activeFeatureSets.put(
                UNLOCK_APP_NO_ADDS,
                FeatureSet.builder()
                        .isAdvertisingEnabled(false)
                        .build()
        );
        activeFeatureSets.put(
                UNLOCK_APP_UP_TO_FIVE_YEAR_RUNTIME,
                FeatureSet.builder()
                        .isTwoYearRuntimeEnabled(true)
                        .isFiveYearRuntimeEnabled(true)
                        .build()
        );
    }

    public void tryConnectBillingClient() {
        if (isConnected)
            return;

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@androidx.annotation.NonNull BillingResult billingResult) {
                Log.i("FeatureManager", "Connection response code is " + billingResult.getResponseCode());
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    isConnected = true;
                    listeners.forEach(next -> next.onConnectionOpened(billingResult));
                    updateProductsPurchasesAndFeatureLists();
                } else {
                    isConnected = false;
                    listeners.forEach(next -> next.onErrorLoadingFeatures());
                    listeners.forEach(next -> next.onConnectionClosed());
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                isConnected = false;
                listeners.forEach(next -> next.onConnectionClosed());
            }
        });
    }

    /**
     * Updates purchase history information.
     */
    public boolean updatePurchases() {

        if (!isConnected())
            return false;

        billingClient.queryPurchases(BillingClient.SkuType.INAPP).getPurchasesList().forEach(purchase -> {
            purchases.put(purchase.getSku(), purchase);
            purchasedFeatures.add(activeFeatureSets.get(purchase.getSku()));
        });

        return true;
    }

    /**
     * Updates list of possible features.
     */
    public boolean updateProductList() {

        if (!isConnected())
            return false;

        SkuDetailsParams params = SkuDetailsParams
                .newBuilder()
                .setSkusList(new ArrayList<>(activeFeatureSets.keySet()))
                .setType(BillingClient.SkuType.INAPP)
                .build();
        billingClient.querySkuDetailsAsync(params, (billingResult, skuDetailsList) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                skuDetailsList.forEach(listing -> {
                    productList.put(listing.getSku(), listing);
                });
                listeners.forEach(next -> next.onProductListReady());
            } else {
                listeners.forEach(next -> next.onErrorLoadingFeatures());
                listeners.forEach(next -> next.onConnectionClosed());
            }
        });
        return true;
    }

    @NotNull
    public Map<SkuDetails, Boolean> getProductDetailsAndOwnership() {
        return productList.values().stream().collect(Collectors.toMap(next -> next, next -> purchases.containsKey(next.getSku())));
    }

    public FeatureSet getPurchasedFeatures(boolean checkForUpdates) {
        if (checkForUpdates)
            updateProductsPurchasesAndFeatureLists();

        FeatureSet features = FeatureSet.noFeatures();
        for (FeatureSet next : purchasedFeatures) {
            features = features.and(next);
        }
        return features;
    }

    public boolean isFeaturePendingOrPurchased(@NonNull String sku) {
        return purchases.containsKey(sku)
                && (purchases.get(sku).getPurchaseState() == Purchase.PurchaseState.PURCHASED
                || purchases.get(sku).getPurchaseState() == Purchase.PurchaseState.PENDING);
    }

    public void listen(@NonNull FeatureManagerListener listener) {
        listeners.add(listener);

        // Run through updates.
        if (isConnected()) {
            Log.i("FeatureManager", "Listener added and FM is connected.");
            listener.onProductListReady();
            listener.onFeaturesUpdated();
        } else {
            Log.i("FeatureManager", "Listener added but FM not connected.");
            tryConnectBillingClient();
        }
    }

    /**
     * Stops the listener from listening.
     *
     * @param listener
     * @return
     * @pre listener was listening
     * @pre listener not null
     */
    public void stopListeneing(@NonNull FeatureManagerListener listener) {
        if (!listeners.contains(listener)) {
            throw new IllegalArgumentException("Listener was not listening to the feature manager.");
        }
        listeners.remove(listener);
    }

    @Override
    public void onPurchasesUpdated(@androidx.annotation.NonNull BillingResult billingResult, @Nullable List<Purchase> list) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && list != null) {

            list.forEach(purchase -> {
                Log.i("FeatureManager", "Acknowledging purchase");
                billingClient.acknowledgePurchase(
                        AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.getPurchaseToken()).build(),
                        result -> {}
                );
            });
            updateProductsPurchasesAndFeatureLists();
        }
    }

    public BillingResult launchBillingFlow(@NonNull Activity activity, @NonNull SkuDetails sku) {
        BillingFlowParams billingFlowParams = BillingFlowParams
                .newBuilder()
                .setSkuDetails(sku)
                .build();
        BillingResult initialResult = billingClient.launchBillingFlow(activity, billingFlowParams);
        return initialResult;
    }

    public interface FeatureManagerListener {
        /**
         * Called when the product list is ready.
         * Boolean indicates whether the product is already owned by the user.
         */
        void onProductListReady();

        default void onConnectionOpened(BillingResult newStatus) {}

        default void onConnectionClosed() {}

        void onFeaturesUpdated();

        default void onErrorLoadingFeatures() {}
    }

    public List<String> getProductsWithFeatures(@NonNull FeatureSet filter) {
        return activeFeatureSets
                .entrySet()
                .stream()
                .filter(idFeatures ->
                        filter.overlaps(idFeatures.getValue())
                )
                .map(idFeatures -> idFeatures.getKey())
                .collect(Collectors.toList());
    }
}
