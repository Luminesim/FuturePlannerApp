package com.example.myfirstapp;

import android.content.Context;

import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.myfirstapp.db.Entity;
import com.example.myfirstapp.db.EntityDao;
import com.example.myfirstapp.db.EntityDatabase;
import com.example.myfirstapp.db.EntityFact;
import com.example.myfirstapp.db.EntityFactDetail;
import com.example.myfirstapp.db.EntityWithFacts;
import com.example.myfirstapp.monad.MonadData;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class DatabaseTest {

    private EntityDao dao;
    private EntityDatabase db;
    private final double DELTA = 0.00001;

    @Before
    public void createDb() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        db = Room.inMemoryDatabaseBuilder(context, EntityDatabase.class).allowMainThreadQueries().build();
        dao = db.entityDao();
    }

    @After
    public void closeDb() throws IOException {
        db.close();
    }

    @Test
    public void removeEntityWithFacts_shouldRemoveFactsAndDetails() {

        // Create test user
        Entity toAdd = Entity.builder().name("User").build();
        long userId = dao.insert(toAdd);

        // Create test facts.
        EntityFact fact1 = EntityFact.builder().category(Category.Income).entityUid(userId).name("Work").build();
        long fact1Uid = dao.insert(fact1);

        // Create test fact details.
        EntityFactDetail fact1Step0 = EntityFactDetail.builder().entityFactUid(fact1Uid).stepNumber(0).monadJson((new MonadData("IdMoneyAmount", 120_000.00)).toJson()).build();
        EntityFactDetail fact1Step1 = EntityFactDetail.builder().entityFactUid(fact1Uid).stepNumber(0).monadJson((new MonadData("IdPerYear")).toJson()).build();
        dao.insert(fact1Step0);
        dao.insert(fact1Step1);

        // Ensure entities and facts exist before we do the deletion.
        assertFalse("Expected entities in the database.", dao.getEntitiesNow().isEmpty());
        assertFalse("Expected entity facts in the database.", dao.getEntityFactsNow().isEmpty());

        // Remove entity.
        dao.delete(dao.getEntity(userId).entity);

        // Ensure there is no entity, entity facts, nor fact details.
        assertTrue("Expected no entities to remain.", dao.getEntitiesNow().isEmpty());
        assertTrue("Expected no entity facts to remain.", dao.getEntityFactsNow().isEmpty());
    }

    /**
     * Ensures that we can record a simple entity with one fact.
     */
    @Test
    public void addEntityWithFacts_shouldPersist() throws IOException {

        // Create test user
        Entity toAdd = Entity.builder().name("User").build();
        long userId = dao.insert(toAdd);

        // Create test facts.
        EntityFact fact1 = EntityFact.builder().category(Category.Income).entityUid(userId).name("Work").build();
        long fact1Uid = dao.insert(fact1);

        // Create test fact details.
        EntityFactDetail fact1Step0 = EntityFactDetail.builder().entityFactUid(fact1Uid).stepNumber(0).monadJson((new MonadData("IdMoneyAmount", 120_000.00)).toJson()).build();
        EntityFactDetail fact1Step1 = EntityFactDetail.builder().entityFactUid(fact1Uid).stepNumber(0).monadJson((new MonadData("IdPerYear")).toJson()).build();
        dao.insert(fact1Step0);
        dao.insert(fact1Step1);

        // Pull it all out and see if everything was retained.
        EntityWithFacts result = dao.getEntity(userId);
        assertEquals("Expected retention of user name.", "User", result.entity.getName());
        assertEquals("Expected correct number of facts.", 1, result.facts.size());
        assertEquals("Expected retention of fact name.", "Work", result.facts.get(0).fact.getName());
        assertEquals("Expected retention of fact category.", Category.Income, result.facts.get(0).fact.getCategory());
        assertEquals("Expected retention of fact details.", 2, result.facts.get(0).details.size());
        assertEquals("Expected retention of fact detail monad parameter (step 0).", 120_000.00, (double) MonadData.fromJson(result.facts.get(0).details.get(0).getMonadJson()).getParameters()[0], DELTA);
        assertEquals("Expected retention of fact detail monad ID (step 0).", "IdMoneyAmount", MonadData.fromJson(result.facts.get(0).details.get(0).getMonadJson()).getMonadId());
        assertEquals("Expected retention of fact detail monad parameter (step 1.", 0, MonadData.fromJson(result.facts.get(0).details.get(1).getMonadJson()).getParameters().length);
        assertEquals("Expected retention of fact detail monad ID (step 1).", "IdPerYear", MonadData.fromJson(result.facts.get(0).details.get(1).getMonadJson()).getMonadId());
    }

    /**
     * Ensures that we can record a simple entity with one fact.
     */
    @Test
    public void updateEntityWithFacts_shouldOverwrite() throws IOException {

        // Create test user
        Entity toAdd = Entity.builder().name("User").build();
        long userId = dao.insert(toAdd);

        // Create test facts.
        EntityFact fact1 = EntityFact.builder().category(Category.Income).entityUid(userId).name("Work").build();
        long fact1Uid = dao.insert(fact1);

        // Create test fact details.
        EntityFactDetail fact1Step0 = EntityFactDetail.builder().entityFactUid(fact1Uid).stepNumber(0).monadJson((new MonadData("IdMoneyAmount", 120_000.00)).toJson()).build();
        EntityFactDetail fact1Step1 = EntityFactDetail.builder().entityFactUid(fact1Uid).stepNumber(0).monadJson((new MonadData("IdPerYear")).toJson()).build();
        dao.insert(fact1Step0);
        dao.insert(fact1Step1);

        // Pull out the entity again.
        EntityWithFacts preEdit = dao.getEntity(userId);

        // Make updates.
        preEdit.facts.get(0).fact.setName("Gigs");
        preEdit.facts.get(0).details.get(0).setMonadJson((new MonadData("IdMoneyAmount", 60_000.00)).toJson());
        dao.insert(preEdit.facts.get(0).fact);
        dao.insert(preEdit.facts.get(0).details.get(0));

        // Pull it out again
        EntityWithFacts postEdit = dao.getEntity(userId);
        assertEquals("Expected fact name update.", "Gigs", postEdit.facts.get(0).fact.getName());
        assertEquals("Expected fact detail monad parameter update (step 0).", 60_000.00, (double) MonadData.fromJson(postEdit.facts.get(0).details.get(0).getMonadJson()).getParameters()[0], DELTA);
        assertEquals("Expected retention of fact detail monad ID (step 0).", "IdMoneyAmount", MonadData.fromJson(postEdit.facts.get(0).details.get(0).getMonadJson()).getMonadId());
        assertEquals("Expected no new fact details.", 2, postEdit.facts.get(0).details.size());
    }

    /**
     * Ensures that we can record a simple entity with no facts.
     */
    @Test
    public void addFactlessEntity_shouldPersist() {

        // Create test data.
        Entity toAdd = Entity.builder().name("User").build();
        int userId = (int) dao.insert(toAdd);

        // Ensure user is recorded.
        EntityWithFacts result = dao.getEntity(userId);
        assertEquals("Expected entity with correct ID.", userId, result.entity.getUid());
        assertTrue("Expected entity with no facts.", result.facts.isEmpty());
    }
//        EntityFact incomeWork = EntityFact
//                .builder()
//                .category(Category.Income)
//                .entityUid(user.getUid())
//                .name("Work")
//                .build();
//        List<EntityFactDetail> details = new LinkedList<>();
//        details.add(EntityFactDetail
//                .builder()
//                .entityFactId(incomeWork.getUid())
//                .stepNumber(0)
//                .monadJson((new MonadData("IdMoneyAmount", 100_000.0)).toJson())
//                .build());
//        details.add(EntityFactDetail
//                .builder()
//                .entityFactId(incomeWork.getUid())
//                .stepNumber(1)
//                .monadJson((new MonadData("IdPerYear")).toJson())
//                .build());
//        EntityWithFacts toAdd = new EntityWithFacts(
//                user,
//                Arrays.asList(new EntityFactWithDetails(incomeWork, details))
//        );
//        dao.insert(toAdd);
//
//        // Get the data back.
//        EntityWithFacts result = dao.getEntity()

}