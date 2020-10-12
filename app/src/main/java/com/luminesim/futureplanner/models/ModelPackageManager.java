package com.luminesim.futureplanner.models;

import com.luminesim.futureplanner.monad.MonadDatabase;

import java.util.HashMap;
import java.util.Map;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PUBLIC)
public class ModelPackageManager {
    @NonNull
    private final MonadDatabase database;

    private Map<String, ModelPackage> packages = new HashMap<>();

    public void enable(@NonNull ModelPackage modelPackage) {
        if (packages.containsKey(modelPackage.getId()))
            return;

        packages.put(modelPackage.getId(), modelPackage);
        modelPackage.getOptionProvider().accept(database);
    }

    private static ModelPackageManager INSTANCE;
    public static synchronized ModelPackageManager gettInstance(MonadDatabase db) {
        if (INSTANCE == null) {
            INSTANCE = new ModelPackageManager(db);
        }
        return INSTANCE;
    }
}
