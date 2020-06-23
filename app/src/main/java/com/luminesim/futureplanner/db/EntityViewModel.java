package com.luminesim.futureplanner.db;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

public class EntityViewModel extends AndroidViewModel {

    private EntityRepository mRepository;

    private LiveData<List<EntityWithFacts>> mAllEntities;

    public EntityViewModel(@NonNull Application application) {
        super(application);
        mRepository = new EntityRepository(application);
        mAllEntities = mRepository.getAllEntities();
    }

    public LiveData<List<EntityWithFacts>> getAllEntities() {
        return mAllEntities;
    }

//    public LiveData<List<EntityFactWithDetails>> getFacts(Entity entity) {
//        return mRepository.getEntityFacts(entity);
//    }
//
//    public void insert(EntityWithFacts entity) {
//        mRepository.insert(entity);
//    }
}
