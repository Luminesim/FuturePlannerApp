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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.time.LocalDate;

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
    public void removeEntityWithFacts_shouldRemoveFactsAndDetails() throws Throwable {

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
        dao.delete(dao.getEntity(userId).getEntity());

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
        assertEquals("Expected retention of user name.", "User", result.getEntity().getName());
        assertEquals("Expected correct number of facts.", 1, result.getFacts().size());
        assertEquals("Expected retention of fact name.", "Work", result.getFacts().get(0).getFact().getName());
        assertEquals("Expected retention of fact category.", Category.Income, result.getFacts().get(0).getFact().getCategory());
        assertEquals("Expected retention of fact details.", 2, result.getFacts().get(0).getDetails().size());
        assertEquals("Expected retention of fact detail monad parameter (step 0).", 120_000.00, Double.valueOf(MonadData.fromJson(result.getFacts().get(0).getDetails().get(0).getMonadJson()).getParameters()[0]), DELTA);
        assertEquals("Expected retention of fact detail monad ID (step 0).", "IdMoneyAmount", MonadData.fromJson(result.getFacts().get(0).getDetails().get(0).getMonadJson()).getMonadId());
        assertEquals("Expected retention of fact detail monad parameter (step 1.", 0, MonadData.fromJson(result.getFacts().get(0).getDetails().get(1).getMonadJson()).getParameters().length);
        assertEquals("Expected retention of fact detail monad ID (step 1).", "IdPerYear", MonadData.fromJson(result.getFacts().get(0).getDetails().get(1).getMonadJson()).getMonadId());
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
        preEdit.getFacts().get(0).getFact().setName("Gigs");
        preEdit.getFacts().get(0).getDetails().get(0).setMonadJson((new MonadData("IdMoneyAmount", 60_000.00)).toJson());
        dao.insert(preEdit.getFacts().get(0).getFact());
        dao.insert(preEdit.getFacts().get(0).getDetails().get(0));

        // Pull it out again
        EntityWithFacts postEdit = dao.getEntity(userId);
        assertEquals("Expected fact name update.", "Gigs", postEdit.getFacts().get(0).getFact().getName());
        assertEquals("Expected fact detail monad parameter update (step 0).", 60_000.00, Double.valueOf(MonadData.fromJson(postEdit.getFacts().get(0).getDetails().get(0).getMonadJson()).getParameters()[0]), DELTA);
        assertEquals("Expected retention of fact detail monad ID (step 0).", "IdMoneyAmount", MonadData.fromJson(postEdit.getFacts().get(0).getDetails().get(0).getMonadJson()).getMonadId());
        assertEquals("Expected no new fact details.", 2, postEdit.getFacts().get(0).getDetails().size());
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
        assertEquals("Expected entity with correct ID.", userId, result.getEntity().getUid());
        assertTrue("Expected entity with no facts.", result.getFacts().isEmpty());
    }

    /**
     * Ensures that we can record an entity with a date-related fact.
     * This is often a source of failure, hence ensuring this works.
     */
    @Test
    public void addDateFact_shouldPersist() throws Throwable {

        // Create test user
        Entity toAdd = Entity.builder().name("User").build();
        long userId = dao.insert(toAdd);

        // Create test facts.
        EntityFact fact1 = EntityFact.builder().category(Category.Income).entityUid(userId).name("Work").build();
        long fact1Uid = dao.insert(fact1);

        // Create test fact details.
        EntityFactDetail fact1Step0 = EntityFactDetail.builder().entityFactUid(fact1Uid).stepNumber(0).monadJson((new MonadData("IdMoneyAmount", 120_000.00)).toJson()).build();
        EntityFactDetail fact1Step1 = EntityFactDetail.builder().entityFactUid(fact1Uid).stepNumber(0).monadJson((new MonadData("IdPerYear")).toJson()).build();
        LocalDate dt = LocalDate.now().withYear(2020).withMonth(6).withDayOfMonth(16);
        EntityFactDetail fact1Step2 = EntityFactDetail.builder().entityFactUid(fact1Uid).stepNumber(0).monadJson((new MonadData("IdStartDate", dt)).toJson()).build();
        dao.insert(fact1Step0);
        dao.insert(fact1Step1);
        dao.insert(fact1Step2);

        // Pull out the date data.
        EntityWithFacts result = dao.getEntity(userId);
        MonadData data = MonadData.fromJson(result.getFacts().get(0).getDetails().get(2).getMonadJson());
        ObjectMapper out = new ObjectMapper();
        out.registerModule(new JavaTimeModule());
        out.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        assertEquals("Expected retention of fact detail monad parameter (step 2).", 1, data.getParameters().length);
        assertEquals("Expected retention of fact detail monad ID (step 2).", "IdStartDate", data.getMonadId());
        LocalDate resultDt = out.readValue(data.getParameters()[0], LocalDate.class);
        assertEquals("Dates should be equal.", dt, resultDt);
    }

}