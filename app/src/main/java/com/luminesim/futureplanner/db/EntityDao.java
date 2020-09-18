package com.luminesim.futureplanner.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

/**
 * Allows all entities and related facts to be found.
 */
@Dao
public interface EntityDao {
    @Transaction
    @Query("SELECT * FROM entities")
    LiveData<List<EntityWithFacts>> getEntities();

    @Transaction
    @Query("SELECT * FROM entities")
    List<EntityWithFacts> getEntitiesNow();

    @Transaction
    @Query("SELECT * FROM entities WHERE uid == (:entityUid)")
    EntityWithFacts getEntity(long entityUid);

    @Transaction
    @Query("SELECT * FROM entity_facts WHERE uid == (:factUid)")
    EntityFactWithDetails getEntityFact(long factUid);

    @Query("SELECT * FROM entity_facts WHERE entity_uid == (:entityUid)")
    List<EntityFact> getFacts(long entityUid);

    @Transaction
    @Query("SELECT * FROM entity_facts")
    LiveData<List<EntityFactWithDetails>> getEntityFacts();

    @Transaction
    @Query("SELECT * FROM entity_facts WHERE entity_uid == (:entity_uid)")
    LiveData<List<EntityFactWithDetails>> getEntityFacts(long entity_uid);

    @Transaction
    @Query("SELECT * FROM entity_facts")
    List<EntityFactWithDetails> getEntityFactsNow();

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Entity entity);

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(EntityParameter parameter);

    @Transaction
    default long insert(EntityWithParameters ewp) {
        long uid = insert(ewp.getEntity());
        ewp.getParameters().forEach(param -> param.setEntityUid(uid));
        ewp.getParameters().forEach(this::insert);
        return uid;
    }

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(EntityFact fact);

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(EntityFactDetail detail);

    @Transaction
    default void delete(EntityFact fact) {
        deleteEntityFactDetails(fact.getUid());
        deleteEntityFact(fact.getUid());
    }

    @Transaction
    default void delete(Entity entity) {
        getFacts(entity.getUid()).forEach(this::delete);
        deleteEntity(entity.getUid());
    }

    @Query("DELETE FROM entities WHERE uid == (:entity_uid)")
    void deleteEntity(long entity_uid);

    @Query("DELETE FROM entity_facts WHERE uid == (:fact_uid)")
    void deleteEntityFact(long fact_uid);


    @Query("DELETE FROM entity_fact_details WHERE entity_fact_uid == (:fact_uid)")
    void deleteEntityFactDetails(long fact_uid);

    default void deleteEntities() {
        getEntitiesNow().forEach(e -> delete(e.getEntity()));
    }

    default void printAll() {
        System.out.println("=== ENTITIES ===");
        System.out.println("uid\tname\ttype");
        if (getEntitiesNow() != null) {
            getEntitiesNow().forEach(e -> System.out.println(String.format(
                    "%s\t%s\t%s", e.getEntity().getUid(), e.getEntity().getName(), e.getEntity().getType()
            )));

            System.out.println("=== FACT DETAILS ===");
            System.out.println("entity name\tfact uid\tfact name\tdetail uid\tjson");
            getEntitiesNow().forEach(e -> {
                if (e.getFacts() != null) {
                    e.getFacts().forEach(f ->
                            f.getDetails().forEach(d ->
                                    System.out.println(String.format(
                                            "%s\t%s\t%s\t%s\t%s",
                                            e.getEntity().getName(),
                                            f.getFact().getUid(),
                                            f.getFact().getName(),
                                            d.getUid(),
                                            d.getMonadJson()
                                    ))
                            )
                    );
                }
            });
        }

    }
}
