package com.luminesim.futureplanner.db;

import android.content.Context;

import androidx.lifecycle.LiveData;

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

    /**
     * Constructor used for testing.
     * @param context
     * @param db
     */
    public EntityRepository(@NonNull Context context, @NonNull EntityDatabase db) {
        mEntityDao = db.entityDao();
        mEntities = mEntityDao.getEntities();
    }

    // Room executes all queries on a separate thread.
    // Observed LiveData will notify the observer when the data has changed.
    LiveData<List<EntityWithFacts>> getAllEntities() {
        return mEntities;
    }

    // You must call this on a non-UI thread or your app will throw an exception. Room ensures
    // that you're not doing any long running operations on the main thread, blocking the UI.
//    public void insert(Entity entity) {
//        submit(() -> {
//            mEntityDao.insert(entity);
//        });
//    }

    /**
     * Inserts an entity with any associated parameters.
     * @param entity
     */
    public void insert(EntityWithParameters entity) {
        submit(() -> {
            mEntityDao.insert(entity);
        });
    }

    public LiveData<List<EntityWithFacts>> getEntities() {
        return mEntityDao.getEntities();
    }

    public void getEntity(long uid, Consumer<EntityWithFacts> callback) {
        submit(() -> {
            callback.accept(mEntityDao.getEntity(uid));
        });
    }
    public void getFact(long uid, Consumer<EntityFactWithDetails> callback) {
        submit(() -> {
            callback.accept(mEntityDao.getEntityFact(uid));
        });
    }
    /**
     * Updates all information associated with the fact (or creates a new one).
     * All prior details (if any) are dumped and replaced.
     * Details will be given the fact's ID, if updated.
     * @pre Entity must exist.
     */
    public void updateFact(long entityUid, @NonNull EntityFact fact, @NonNull List<EntityFactDetail> details, Runnable onSaved) {

        submit(() -> {
            // Get the entity. Ensure it exists.
            EntityWithFacts entity = mEntityDao.getEntity(entityUid);
            if (entity == null) {
                throw new RuntimeException(String.format("Entity %s not found", entityUid));
            }

            // Update or get the new fact.
            long factUid = mEntityDao.insert(fact);
            details.forEach(detail -> detail.setEntityFactUid(factUid));

            // Delete all associated fact details as we'll be replacing them, then
            // ensure the entity is in fact updated.
            mEntityDao.deleteEntityFactDetails(factUid);

            // Add all details.
            details.forEach(mEntityDao::insert);

            // Callback.
            onSaved.run();
        });
    }

    /**
     * Deletes all database items.
     */
    public void deleteAll() {
        submit(() -> {
            mEntityDao.deleteEntities();
        });
    }

    /**
     * Deletes the given fact and associated details.
     * @param factUid
     *  The ID of the fact.
     */
    public void deleteFact(long factUid) {
        submit(() -> {
            mEntityDao.deleteEntityFact(factUid);
            mEntityDao.deleteEntityFactDetails(factUid);
        });
    }

    /**
     * Submits a job to do in a separate thread.
     * @param asyncAction
     *  The action to run.
     * @pre asyncAction != null
     */
    private void submit(@NonNull Runnable asyncAction) {
        EntityDatabase.databaseWriteExecutor.execute(asyncAction);
    }

    public void updateParameter(long entityUid, EntityParameter param) {
        submit(() -> {
            mEntityDao.insert(param);
        });
    }
}
