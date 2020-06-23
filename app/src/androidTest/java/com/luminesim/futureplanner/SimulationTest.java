package com.luminesim.futureplanner;

import android.content.Context;
import android.util.Log;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.testing.TestListenableWorkerBuilder;
import androidx.work.testing.TestWorkerBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.luminesim.futureplanner.db.Entity;
import com.luminesim.futureplanner.db.EntityDao;
import com.luminesim.futureplanner.db.EntityDatabase;
import com.luminesim.futureplanner.db.EntityFact;
import com.luminesim.futureplanner.db.EntityFactDetail;
import com.luminesim.futureplanner.db.EntityFactWithDetails;
import com.luminesim.futureplanner.db.EntityRepository;
import com.luminesim.futureplanner.db.EntityWithFacts;
import com.luminesim.futureplanner.monad.MonadData;
import com.luminesim.futureplanner.simulation.SimulationWorker;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class SimulationTest {
    private Context mContext;
    private EntityDao dao;
    private EntityWithFacts mEntity;
    private EntityDatabase db;
    private final double DELTA = 0.00001;

    public void createDb() {
        db = Room.inMemoryDatabaseBuilder(mContext, EntityDatabase.class).allowMainThreadQueries().build();
        dao = db.entityDao();
    }

    @After
    public void closeDb() throws IOException {
        db.close();
    }

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        createDb();

        // Create test user
        Entity toAdd = Entity.builder().name("User").build();
        long userId = dao.insert(toAdd);

        // Create test facts.
        EntityFact incomeFact = EntityFact.builder().category(Category.Income).entityUid(userId).name("Work").build();
        EntityFact expenseFact = EntityFact.builder().category(Category.Expenses).entityUid(userId).name("Food").build();
        long incomeFactUid = dao.insert(incomeFact);
        long expenseFactUid = dao.insert(expenseFact);

        // Create test fact details.
        EntityFactDetail incomeFactStep0 = EntityFactDetail.builder().entityFactUid(incomeFactUid).stepNumber(0).monadJson((new MonadData("IdMoneyAmount", 120_000.00)).toJson()).build();
        EntityFactDetail incomeFactStep1 = EntityFactDetail.builder().entityFactUid(incomeFactUid).stepNumber(1).monadJson((new MonadData("IdPerYear")).toJson()).build();
        dao.insert(incomeFactStep0);
        dao.insert(incomeFactStep1);
        EntityFactDetail expenseFactStep0 = EntityFactDetail.builder().entityFactUid(expenseFactUid).stepNumber(0).monadJson((new MonadData("IdMoneyAmount", 200.00)).toJson()).build();
        EntityFactDetail expenseFactStep1 = EntityFactDetail.builder().entityFactUid(expenseFactUid).stepNumber(1).monadJson((new MonadData("IdPerWeek")).toJson()).build();
        dao.insert(expenseFactStep0);
        dao.insert(expenseFactStep1);

        mEntity = dao.getEntity(userId);
    }

    @Test
    public void testSleepWorker() throws Exception {
        Log.i("RESULT", "User UID is " + mEntity.getEntity().getUid());
        SimulationWorker worker =
                (SimulationWorker)TestWorkerBuilder.from(mContext, SimulationWorker.class)
                        .setInputData(new Data.Builder().putLong(SimulationWorker.DATA_ENTITY_UID, mEntity.getEntity().getUid()).build())
                        .build();

        // Test hook.
        worker.setRepo(new EntityRepository(mContext, db));

        ListenableWorker.Result result = worker.startWork().get();

        // Ensure that funds at the end of the run are what we expect.
        assertThat("Expected successful run.", result, is(ListenableWorker.Result.success()));
        assertEquals("Expected the correct income calculation.", 72_216.76, (Double)worker.getRoot().getNumericSDDiagram("Money").get("Funds"), 100.0);


        Log.i("RESULT", result.toString());
    }
}