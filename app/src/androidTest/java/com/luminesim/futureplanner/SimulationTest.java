package com.luminesim.futureplanner;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.luminesim.futureplanner.db.Entity;
import com.luminesim.futureplanner.db.EntityDao;
import com.luminesim.futureplanner.db.EntityDatabase;
import com.luminesim.futureplanner.db.EntityFact;
import com.luminesim.futureplanner.db.EntityFactDetail;
import com.luminesim.futureplanner.db.EntityRepository;
import com.luminesim.futureplanner.db.EntityWithFacts;
import com.luminesim.futureplanner.monad.MonadData;
import com.luminesim.futureplanner.simulation.CanadianIndividualIncomeSimulation;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

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
    private final double DELTA = 500.0;

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
    }

    private EntityWithFacts create120KIncomePerson() {
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
//        EntityFactDetail expenseFactStep0 = EntityFactDetail.builder().entityFactUid(expenseFactUid).stepNumber(0).monadJson((new MonadData("IdMoneyAmount", 200.00)).toJson()).build();
//        EntityFactDetail expenseFactStep1 = EntityFactDetail.builder().entityFactUid(expenseFactUid).stepNumber(1).monadJson((new MonadData("IdPerWeek")).toJson()).build();
//        dao.insert(expenseFactStep0);
//        dao.insert(expenseFactStep1);

        return dao.getEntity(userId);
    }

    private EntityWithFacts create50KIncomePerson() {
        // Create test user
        Entity toAdd = Entity.builder().name("User").build();
        long userId = dao.insert(toAdd);

        // Create test facts.
        EntityFact incomeFact = EntityFact.builder().category(Category.Income).entityUid(userId).name("Work").build();
        EntityFact expenseFact = EntityFact.builder().category(Category.Expenses).entityUid(userId).name("Food").build();
        long incomeFactUid = dao.insert(incomeFact);
        long expenseFactUid = dao.insert(expenseFact);

        // Create test fact details.
        EntityFactDetail incomeFactStep0 = EntityFactDetail.builder().entityFactUid(incomeFactUid).stepNumber(0).monadJson((new MonadData("IdMoneyAmount", 50_000.00)).toJson()).build();
        EntityFactDetail incomeFactStep1 = EntityFactDetail.builder().entityFactUid(incomeFactUid).stepNumber(1).monadJson((new MonadData("IdPerYear")).toJson()).build();
        dao.insert(incomeFactStep0);
        dao.insert(incomeFactStep1);
//        EntityFactDetail expenseFactStep0 = EntityFactDetail.builder().entityFactUid(expenseFactUid).stepNumber(0).monadJson((new MonadData("IdMoneyAmount", 200.00)).toJson()).build();
//        EntityFactDetail expenseFactStep1 = EntityFactDetail.builder().entityFactUid(expenseFactUid).stepNumber(1).monadJson((new MonadData("IdPerWeek")).toJson()).build();
//        dao.insert(expenseFactStep0);
//        dao.insert(expenseFactStep1);

        return dao.getEntity(userId);
    }

    private EntityWithFacts create10KIncomePerson() {
        // Create test user
        Entity toAdd = Entity.builder().name("User").build();
        long userId = dao.insert(toAdd);

        // Create test facts.
        EntityFact incomeFact = EntityFact.builder().category(Category.Income).entityUid(userId).name("Work").build();
        EntityFact expenseFact = EntityFact.builder().category(Category.Expenses).entityUid(userId).name("Food").build();
        long incomeFactUid = dao.insert(incomeFact);
        long expenseFactUid = dao.insert(expenseFact);

        // Create test fact details.
        EntityFactDetail incomeFactStep0 = EntityFactDetail.builder().entityFactUid(incomeFactUid).stepNumber(0).monadJson((new MonadData("IdMoneyAmount", 10_000.00)).toJson()).build();
        EntityFactDetail incomeFactStep1 = EntityFactDetail.builder().entityFactUid(incomeFactUid).stepNumber(1).monadJson((new MonadData("IdPerYear")).toJson()).build();
        dao.insert(incomeFactStep0);
        dao.insert(incomeFactStep1);
//        EntityFactDetail expenseFactStep0 = EntityFactDetail.builder().entityFactUid(expenseFactUid).stepNumber(0).monadJson((new MonadData("IdMoneyAmount", 200.00)).toJson()).build();
//        EntityFactDetail expenseFactStep1 = EntityFactDetail.builder().entityFactUid(expenseFactUid).stepNumber(1).monadJson((new MonadData("IdPerWeek")).toJson()).build();
//        dao.insert(expenseFactStep0);
//        dao.insert(expenseFactStep1);

        return dao.getEntity(userId);
    }

    /**
     * Runs a simple simulation with a single 120K income.
     * Should be able to calculate this amount.
     *
     * @throws Exception
     */
    @Test
    public void simpleCAD120KIncome_shouldCalculateCorrectAmount() throws Exception {
        // Set up the simulation.
        mEntity = create120KIncomePerson();
        CanadianIndividualIncomeSimulation job = new CanadianIndividualIncomeSimulation(mContext, mEntity.getEntity().getUid());
        job.setRepo(new EntityRepository(mContext, db));
        job.run();

        // Ensure that funds at the end of the run are what we expect.
        assertEquals("Expected the correct income calculation.", 83_566, (Double)job.getRoot().getNumericSDDiagram("Money").get("Funds"), DELTA);
    }

    /**
     * Runs a simple simulation with a single 50K income.
     * Should be able to calculate this amount.
     *
     * @throws Exception
     */
    @Test
    public void simpleCAD50KIncome_shouldCalculateCorrectAmount() throws Exception {
        // Set up the simulation.
        mEntity = create50KIncomePerson();
        CanadianIndividualIncomeSimulation job = new CanadianIndividualIncomeSimulation(mContext, mEntity.getEntity().getUid());
        job.setRepo(new EntityRepository(mContext, db));
        job.run();

        // Ensure that funds at the end of the run are what we expect.
        assertEquals("Expected the correct income calculation.", 38_339, (Double)job.getRoot().getNumericSDDiagram("Money").get("Funds"), DELTA);
    }

    /**
     * Runs a simple simulation with a single 10K income.
     * Should be able to calculate this amount.
     *
     * @throws Exception
     */
    @Test
    public void simpleCAD10KIncome_shouldCalculateCorrectAmount() throws Exception {
        // Set up the simulation.
        mEntity = create10KIncomePerson();
        CanadianIndividualIncomeSimulation job = new CanadianIndividualIncomeSimulation(mContext, mEntity.getEntity().getUid());
        job.setRepo(new EntityRepository(mContext, db));
        job.run();

        // Ensure that funds at the end of the run are what we expect.
        assertEquals("Expected the correct income calculation.", 9_507, (Double)job.getRoot().getNumericSDDiagram("Money").get("Funds"), DELTA);
    }
}