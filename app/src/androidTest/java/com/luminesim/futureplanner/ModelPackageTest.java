package com.luminesim.futureplanner;

import android.app.Instrumentation;
import android.content.Context;

import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.luminesim.futureplanner.db.Entity;
import com.luminesim.futureplanner.db.EntityDao;
import com.luminesim.futureplanner.db.EntityDatabase;
import com.luminesim.futureplanner.db.EntityRepository;
import com.luminesim.futureplanner.models.AssetType;
import com.luminesim.futureplanner.models.Model;
import com.luminesim.futureplanner.models.ModelPackage;
import com.luminesim.futureplanner.models.ModelTemplate;
import com.luminesim.futureplanner.models.ModelView;
import com.luminesim.futureplanner.models.Qualifier;
import com.luminesim.futureplanner.monad.MonadDatabase;
import com.luminesim.futureplanner.simulation.SimpleIndividualIncomeSimulation;

import org.hamcrest.core.Is;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Currency;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import ca.anthrodynamics.indes.Engine;
import ca.anthrodynamics.indes.abm.Agent;
import ca.anthrodynamics.indes.lang.Monad;
import ca.anthrodynamics.indes.lang.Traits;
import ca.anthrodynamics.indes.sd.SDDiagram;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import static org.junit.Assert.assertEquals;

/**
 * Tests {@link com.luminesim.futureplanner.models.ModelPackage}
 */
@RunWith(AndroidJUnit4.class)
public class ModelPackageTest {

    private EntityDatabase db;
    private long userId;

    @Before
    public void createDb() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        db = Room.inMemoryDatabaseBuilder(context, EntityDatabase.class).allowMainThreadQueries().build();

        // Create test user
        Entity toAdd = Entity.builder().name("User").type(SimpleIndividualIncomeSimulation.ENTITY_TYPE).build();
        userId = db.entityDao().insert(toAdd);
    }

    @After
    public void closeDb() throws IOException {
        db.close();
    }
    @Test
    public void basicAPITest() throws Throwable {
        final String Base = "com.luminesim.futureplanner.models.BassDiffusionTest.";
        Consumer<MonadDatabase> optionProvider = db -> {
            db.add(
                    Base + "BassDiffusionModel",
                    new BassDiffusionModelMonad(),
                    Arrays.asList(Category.ModelDefinition),
                    "is a BDM with",
                    "is a BDM with",
                    "A hint");
            db.add(
                    Base + "PotentialAdopters",
                    new PotentialAdoptersMonad(),
                    Arrays.asList(Category.ModelDefinition),
                    "%s potential adopters",
                    "<number> potential adopters",
                    "A hint");
            db.add(
                    Base + "CurrentAdopters",
                    new CurrentAdoptersMonad(),
                    Arrays.asList(Category.ModelDefinition),
                    "%s current adopters",
                    "<number> current adopters",
                    "A hint");
        };
        // A default generic customer set.
        ModelTemplate defaultTemplate = new ModelTemplate(
                "Product Adoption",
                "Customer",
                new ModelTemplate.AssetDefinition("Customer"),
                Arrays.asList(
                        new ModelTemplate.SetupInstructions(
                                new ModelTemplate.SetupStep(Base + "BassDiffusionModel", "Customer"),
                                new ModelTemplate.SetupStep(Base + "PotentialAdopters", 1000),
                                new ModelTemplate.SetupStep(Base + "CurrentAdopters", 1)
                        )
                )
        );
        Map<String, ModelTemplate> alternativeTemplates = new HashMap<>();

        // Interest-specific stuff.
        // This would produce two sub-models: one for mosh pit, one for seated customers.
        // E.g. Concert (Area: Mosh Pit) and Concert (Area: Seated)
        // Other ideas e.g. HEMA Club (Gender: Male) and HEMA Club (Gender: Female)
        // See Model.compose()
        alternativeTemplates.put("idConcert", new ModelTemplate(
                "Concert Ticket Sales",
                "Customer",
                new ModelTemplate.AssetDefinition(
                        new AssetType("Concert-Goer"),
                        map("Area", set(new Qualifier("Mosh Pit"), new Qualifier("Seated")))),
                Arrays.asList(
                        new ModelTemplate.SetupInstructions(
                                map("Area", new Qualifier("Mosh Pit")),
                                new ModelTemplate.SetupStep(Base + "BassDiffusionModel", "Concert-Goer"),
                                new ModelTemplate.SetupStep(Base + "PotentialAdopters", 500),
                                new ModelTemplate.SetupStep(Base + "CurrentAdopters", 0)
                        ),
                        new ModelTemplate.SetupInstructions(
                                map("Area", new Qualifier("Seated")),
                                new ModelTemplate.SetupStep(Base + "BassDiffusionModel", "Concert-Goer"),
                                new ModelTemplate.SetupStep(Base + "PotentialAdopters", 2000),
                                new ModelTemplate.SetupStep(Base + "CurrentAdopters", 0)
                        )
                )
        ));
        ModelPackage bassDiffusionPackage = new ModelPackage(
                IsCompleteBassDiffusionModel.class,
                defaultTemplate,
                alternativeTemplates,
                optionProvider
        );
        Lock endLock = new ReentrantLock();
        Condition awaitResults = endLock.newCondition();
        AtomicInteger actions = new AtomicInteger(0);
        bassDiffusionPackage.addDefaultTemplateToEntity(
                userId,
                new EntityRepository(InstrumentationRegistry.getInstrumentation().getContext(), db),
                factUid -> {
                    endLock.lock();
                    db.entityDao().printAll();
                    actions.incrementAndGet();
                    awaitResults.signal();
                    endLock.unlock();
                });
        bassDiffusionPackage.addTemplateToEntity(
                "idConcert",
                userId,
                new EntityRepository(InstrumentationRegistry.getInstrumentation().getContext(), db),
                factUid -> {
                    endLock.lock();
                    db.entityDao().printAll();
                    actions.incrementAndGet();
                    awaitResults.signal();
                    endLock.unlock();
                });

        endLock.lock();
        while (actions.get() < 2) {
            awaitResults.await();
        }
        endLock.unlock();
    }

    private <A, B> Map<A, B> map(A a, B b) {
        Map<A, B> map = new HashMap<>();
        map.put(a, b);
        return map;
    }

    private <T> Set<T> set(T... items) {
        return new HashSet<>(Arrays.asList(items));
    }

    private interface IsCompleteBassDiffusionModel extends IsPartialBassDiffusionModel, HasCurrentAdopters, HasPotentialAdopters {}

    private interface IsPartialBassDiffusionModel extends ModelView {
        List<Agent> asAgents(Engine engine, double sdDiagramDt);
    }



    private interface HasPotentialAdopters {
        int getNumberOfPotentialAdopters();
    }

    private interface HasCurrentAdopters {
        int getNumberOfCurrentAdopters();
    }

    private class PotentialAdoptersMonad extends Monad<IsPartialBassDiffusionModel, HasPotentialAdopters> {
        public PotentialAdoptersMonad() {
            super(IsPartialBassDiffusionModel.class, HasPotentialAdopters.class, in -> !in.getProperties().canDuckTypeAs(HasPotentialAdopters.class));
        }

        @Override
        protected Traits apply(Traits traits, Object[] objects) {
            return traits.andThen((HasPotentialAdopters) () -> (int) objects[0]);
        }
    }

    private class CurrentAdoptersMonad extends Monad<IsPartialBassDiffusionModel, HasCurrentAdopters> {
        public CurrentAdoptersMonad() {
            super(IsPartialBassDiffusionModel.class, HasCurrentAdopters.class, in -> !in.getProperties().canDuckTypeAs(HasCurrentAdopters.class));
        }

        @Override
        protected Traits apply(Traits traits, Object[] objects) {
            return traits.andThen((HasPotentialAdopters) () -> (int) objects[0]);
        }
    }

    private class BassDiffusionModelMonad extends Monad<Monad.None, IsPartialBassDiffusionModel> {
        public BassDiffusionModelMonad() {
            super(None.class, IsPartialBassDiffusionModel.class, in -> !in.getProperties().canDuckTypeAs(HasPotentialAdopters.class));
        }

        @Override
        protected Traits apply(Traits traits, Object[] objects) {
            String assetName = (String)objects[0];
            return traits.andThen(new IsPartialBassDiffusionModel() {

                private AssetType assetType = new AssetType(assetName);
                private final String AdopterQualifier = "Status";
                private final Qualifier PotentialAdopter = new Qualifier("Potential "+assetName);
                private final Qualifier CurrentAdopter = new Qualifier("Current "+assetName);
                private Agent self;

                private Agent getSelf(Engine engine, double sdDiagramDt) {
                    if (self != null) {
                        return self;
                    }
                    self = new Agent(engine);
                    SDDiagram<Double> sd = self.addSDDiagram("Bass Diffusion Model", sdDiagramDt);
                    sd
                            .addStock(PotentialAdopter.getLabel(), (double)traits.lazyAs(HasPotentialAdopters.class).getNumberOfPotentialAdopters())
                            .addStock(CurrentAdopter.getLabel(), (double)traits.lazyAs(HasCurrentAdopters.class).getNumberOfCurrentAdopters())
                            .addVariable("p(adAdoption)", () -> 0.0001)
                            .addVariable("p(womAdoption)", () -> 0.1)
                            .addFlow("adoptionRate")
                            .from(PotentialAdopter.getLabel())
                            .to(CurrentAdopter.getLabel())
                            .at((p,c) -> sd.get("p(adAdoption") * p
                                    + (sd.get("p(womAdoption") * c / (p + c)) * p);

                    self.start();
                    return self;
                }


                @Override
                public List<Agent> asAgents(Engine engine, double sdDiagramDt) {
                    return Arrays.asList(getSelf(engine, sdDiagramDt));
                }

                @Override
                public Set<AssetType> getAssetTypes() {
                    return new HashSet<>(Arrays.asList(assetType));
                }

                @Override
                public Map<String, Set<Qualifier>> getAssetQualifiers(@NonNull AssetType assetType) {
                    if (this.assetType.equals(assetType)) {
                        return map(AdopterQualifier, set(PotentialAdopter, CurrentAdopter));
                    }
                    else {
                        return Collections.emptyMap();
                    }
                }

                @Override
                public double getCount(@NonNull AssetType assetType, @NonNull Map<String, Set<Qualifier>> qualifiers) {
                    double sum = 0;

                    // If the query is relevant to the items in this model, return the value.
                    if (this.assetType.equals(assetType)) {
                        if (qualifiers.containsKey(AdopterQualifier)) {
                            Set<Qualifier> adopterTypes = qualifiers.get(AdopterQualifier);
                            if (adopterTypes.contains(PotentialAdopter)) {
                                sum += (Double)self.getNumericSDDiagram("Bass Diffusion Model").get(PotentialAdopter.getLabel());
                            }
                            if (adopterTypes.contains(CurrentAdopter)) {
                                sum += (Double)self.getNumericSDDiagram("Bass Diffusion Model").get(CurrentAdopter.getLabel());
                            }
                        }
                    }
                    return sum;
                }

                @Override
                public ModelView getRootModel() {
                    return null;
                }
            });
        }
    }
}