package com.example.myfirstapp.db;

import android.app.Application;
import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.myfirstapp.R;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import lombok.NonNull;

public class EntityRepository {

    private EntityDao mEntityDao;

    private LiveData<List<EntityWithFacts>> mEntities;

    // Note that in order to unit test the WordRepository, you have to remove the Application
    // dependency. This adds complexity and much more code, and this sample is not about testing.
    // See the BasicSample in the android-architecture-components repository at
    // https://github.com/googlesamples
    public EntityRepository(Context context) {
        this(context, EntityDatabase.getDatabase(context));
    }

    private EntityRepository(@NonNull Context context, @NonNull EntityDatabase db) {
        mEntityDao = db.entityDao();
        mEntities = mEntityDao.getEntities();
    }

    // Room executes all queries on a separate thread.
    // Observed LiveData will notify the observer when the data has changed.
    LiveData<List<EntityWithFacts>> getAllEntities() {
        return mEntities;
    }

//    LiveData<List<EntityFactWithDetails>> getEntityFacts(Entity entity) {
//        return mEntityDao.getEntityFacts(entity.getUid());
//    }

    // You must call this on a non-UI thread or your app will throw an exception. Room ensures
    // that you're not doing any long running operations on the main thread, blocking the UI.
    public void insert(Entity entity) {
        EntityDatabase.databaseWriteExecutor.execute(() -> {
            mEntityDao.insert(entity);
        });
    }

    public LiveData<List<EntityWithFacts>> getEntities() {
        return mEntityDao.getEntities();
    }

    public void getEntity(long uid, Consumer<EntityWithFacts> callback) {
        EntityDatabase.databaseWriteExecutor.execute(() -> {
            callback.accept(mEntityDao.getEntity(uid));
        });
    }
    public void getFact(long uid, Consumer<EntityFactWithDetails> callback) {
        EntityDatabase.databaseWriteExecutor.execute(() -> {
            callback.accept(mEntityDao.getEntityFact(uid));
        });
    }
    /**
     * Updates all information associated with the fact (or creates a new one).
     * All prior details (if any) are dumped and replaced.
     * @pre Entity must exist.
     */
    public void updateFact(@NonNull long entityUid, @NonNull EntityFact fact, @NonNull List<EntityFactDetail> details) {

        EntityDatabase.databaseWriteExecutor.execute(() -> {
            // Get the entity. Ensure it exists.
            EntityWithFacts entity = mEntityDao.getEntity(entityUid);
            if (entity == null) {
                throw new RuntimeException(String.format("Entity %s not found", entityUid));
            }

            // Update or get the new fact.
            long factUid = mEntityDao.insert(fact);

            // Delete all associated fact details as we'll be replacing them, then
            // ensure the entity is in fact updated.
            mEntityDao.deleteEntityFactDetails(factUid);

            // Add all details.
            details.forEach(mEntityDao::insert);
        });
    }
}
