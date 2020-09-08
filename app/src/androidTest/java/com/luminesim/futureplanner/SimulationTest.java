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
import com.luminesim.futureplanner.db.EntityParameter;
import com.luminesim.futureplanner.db.EntityRepository;
import com.luminesim.futureplanner.db.EntityWithFacts;
import com.luminesim.futureplanner.monad.MonadData;
import com.luminesim.futureplanner.simulation.SimpleIndividualIncomeSimulation;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
    private EntityWithFacts mPerson;
    private EntityDatabase db;
    final double Tol = 500.00;
    private SimpleIndividualIncomeSimulation mSimulation;

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
        Entity toAdd = Entity.builder().name("User").type(SimpleIndividualIncomeSimulation.ENTITY_TYPE).build();
        long userId = dao.insert(toAdd);
        EntityParameter param = EntityParameter
                .builder()
                .entityUid(userId)
                .name(SimpleIndividualIncomeSimulation.PARAMETER_INITIAL_FUNDS)
                .value("0.0")
                .build();
        dao.insert(param);
        mPerson = dao.getEntity(userId);

    }

    public double getFunds() {
        return (Double) mSimulation.getRoot().getNumericSDDiagram("Money").get("Funds");
    }

    @Test
    public void singleIncomePA_noExpenses_shouldCalculateCorrectAmount() {
        // Person parameters.
        addIncome("Income").amount(100_000.00).perYear();
        runForOneYear();

        // Ensure income correctly calculated.
        final double Tol = 500.00;
        assertEquals("Funds incorrect", 100_000.00, getFunds(), Tol);
    }

    private void runForOneYear() {
        int daysToRun = 365;
        mSimulation = new SimpleIndividualIncomeSimulation(daysToRun, mContext, mPerson.getEntity().getUid());
        mSimulation.setRepo(new EntityRepository(mContext, db));
        mSimulation.run();
    }

    @Test
    public void singleIncomePM_noExpenses_shouldCalculateCorrectAmount() {
        // Person parameters.
        addIncome("Income").amount(100_000.00 / 12.0).perMonth();
        runForOneYear();

        // Ensure income correctly calculated.
        assertEquals("Funds incorrect", 100_000.00, getFunds(), Tol);
    }

    @Test
    public void singleIncomePF_noExpenses_shouldCalculateCorrectAmount() {
        // Person parameters.
        addIncome("Income").amount(100_000.00 / 26.0).perFortnight();
        runForOneYear();

        // Ensure income correctly calculated.
        assertEquals("Funds incorrect", 100_000.00, getFunds(), Tol);
    }

    @Test
    public void multiIncome_multiExpenses_shouldCalculateCorrectAmount() {
        // Person parameters.
        addIncome("Income").amount(100_000.00).perYear().starting(nDays(365 / 2)); // -> $50K
        addExpense("Rent").amount(2000).perMonth().ending(nDays(365 / 2)); // -> $50K - 12K = 38K
        addExpense("Food").amount(2000).perMonth(); // -> $38K - 24K = 14K
        addIncome("Gigs").amount(100.00).perDay().ending(nDays(100)); // -> 14K + $10K = 24k
        runForOneYear();

        // Ensure income correctly calculated.
        assertEquals("Funds incorrect", 24_000.00, getFunds(), Tol);
    }


    @Test
    public void flowsStartBeforeSimulation_shouldCalculateCorrectAmount() {
        // Person parameters.
        addIncome("Income").amount(100_000.00).perYear().starting(nDays(-1 * 365));
        addExpense("Expense").amount(50_000.00).perYear().starting(nDays(-1 * 365));
        runForOneYear();

        // Ensure income correctly calculated.
        assertEquals("Funds incorrect", 50_000.00, getFunds(), Tol);
    }

    @Test
    public void flowsStartAfterSimulation_shouldCalculateCorrectAmount() {
        // Person parameters.
        addIncome("Income").amount(100_000.00).perYear().starting(nDays(2 + 365));
        addIncome("Expense").amount(50_000.00).perYear().starting(nDays(2 + 365));
        runForOneYear();

        // Ensure income correctly calculated.
        assertEquals("Funds incorrect", 0.00, getFunds(), Tol);
    }

    @Test
    public void flowsHaveInvertedStartEnd_shouldCalculateCorrectAmount() {
        // Person parameters.
        addIncome("Income").amount(100_000.00).perYear().starting(nDays(30)).ending(nDays(10));
        addIncome("Expense").amount(50_000.00).perYear().starting(nDays(30)).ending(nDays(10));
        runForOneYear();

        // Ensure income correctly calculated.
        assertEquals("Funds incorrect", 0.00, getFunds(), Tol);
    }

    @Test
    public void flowsHaveMinusAndPlusBeforeAfterRate_shouldCalculateCorrectAmount() {
        // Person parameters.
        addIncome("Income")
                .amount(100_000.00) // 100k
                .preRatePlus(50.0) // 150k
                .preRateMinus(50.0) // 75k
                .perYear()
                .postRateMinus(33.00) // 50k
                .postRateMinus(50.0) // 25k
                .postRatePlus(50) // 37.5k
                .starting(nDays(365/2)); // 18.75k
        runForOneYear();

        // Ensure income correctly calculated.
        assertEquals("Funds incorrect", 18_750.00, getFunds(), Tol);
    }

    @Test
    public void onDateHasMinusAndPlusBeforeAfterRate_shouldCalculateCorrectAmount() {
        // Person parameters.
        addIncome("Income")
                .amount(100_000.00) // 100k
                .preRatePlus(50.0) // 150k
                .preRateMinus(50.0) // 75k
                .preRateMinus(33.0) // 50K
                .once(nDays(123));
        runForOneYear();

        // Ensure income correctly calculated.
        assertEquals("Funds incorrect", 50_000.00, getFunds(), Tol);
    }

    private LocalDateTime nDays(int n) {
        return LocalDateTime.now().plus(n, ChronoUnit.DAYS);
    }

    private FactBuilder addIncome(String name) {
        return new FactBuilder(Category.Income, name);
    }

    private FactBuilder addExpense(String name) {
        return new FactBuilder(Category.Expenses, name);
    }

    private class FactBuilder {

        private long factUid;
        private AtomicInteger stepNumber = new AtomicInteger(0);

        private FactBuilder(Category category, String name) {
            EntityFact toAdd = EntityFact.builder()
                    .category(category)
                    .entityUid(mPerson.getEntity().getUid())
                    .name(name)
                    .build();
            factUid = dao.insert(toAdd);
        }

        private EntityFactDetail detail(MonadData data) {
            return EntityFactDetail.builder().entityFactUid(factUid).stepNumber(stepNumber.getAndIncrement()).monadJson(data.toJson()).build();
        }

        private FactBuilder amount(double amount) {
            dao.insert(detail(new MonadData("IdMoneyAmount", amount)));
            return this;
        }

        private FactBuilder perYear() {
            dao.insert(detail(new MonadData("IdPerYear")));
            return this;

        }

        private FactBuilder perMonth() {
            dao.insert(detail(new MonadData("IdPerMonth")));
            return this;
        }

        private FactBuilder perFortnight() {
            dao.insert(detail(new MonadData("IdPerFortnight")));
            return this;
        }

        private FactBuilder starting(LocalDateTime dt) {
            dao.insert(detail(new MonadData("IdStarting", dt)));
            return this;
        }

        private FactBuilder ending(LocalDateTime dt) {
            dao.insert(detail(new MonadData("IdEnding", dt)));
            return this;
        }

        private FactBuilder once(LocalDateTime dt) {
            dao.insert(detail(new MonadData("IdOnDate", dt)));
            return this;
        }

        public FactBuilder perDay() {
            dao.insert(detail(new MonadData("IdPerDay")));
            return this;
        }

        public FactBuilder preRateMinus(double percent) {
            dao.insert(detail(new MonadData("IdPercentDeduction", percent)));
            return this;
        }

        public FactBuilder postRateMinus(double percent) {
            dao.insert(detail(new MonadData("IdPercentRateToRate", percent)));
            return this;
        }

        public FactBuilder preRatePlus(double percent) {
            dao.insert(detail(new MonadData("IdPercentAddition", percent)));
            return this;
        }

        public FactBuilder postRatePlus(double percent) {
            dao.insert(detail(new MonadData("IdPercentAdditionRateToRate", percent)));
            return this;
        }
    }
}