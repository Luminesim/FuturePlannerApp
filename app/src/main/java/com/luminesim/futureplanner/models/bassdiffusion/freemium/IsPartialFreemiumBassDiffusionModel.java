package com.luminesim.futureplanner.models.bassdiffusion.freemium;

import android.util.Log;

import com.luminesim.futureplanner.models.AssetType;
import com.luminesim.futureplanner.models.Model;
import com.luminesim.futureplanner.models.ModelView;
import com.luminesim.futureplanner.models.Qualifier;
import com.luminesim.futureplanner.models.bassdiffusion.HasAdopterLifespan;
import com.luminesim.futureplanner.models.bassdiffusion.HasAdopterRetention;
import com.luminesim.futureplanner.models.bassdiffusion.HasConversionFromAds;
import com.luminesim.futureplanner.models.bassdiffusion.HasPotentialAdopters;
import com.luminesim.futureplanner.models.bassdiffusion.HasSpreadThroughContacts;
import com.luminesim.futureplanner.monad.types.SuppliesRootModel;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import ca.anthrodynamics.indes.Engine;
import ca.anthrodynamics.indes.abm.Agent;
import ca.anthrodynamics.indes.lang.Monad;
import ca.anthrodynamics.indes.lang.Traits;
import ca.anthrodynamics.indes.sd.SDDiagram;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * A freemium bass diffusion model where customers can be potential or current adopters; current
 * adopters can further be divided into freemium or paid.
 */
public interface IsPartialFreemiumBassDiffusionModel extends Supplier<Model> {

    static Monad create() {
        return new Monad(SuppliesRootModel.class, IsPartialFreemiumBassDiffusionModel.class) {
            @Override
            protected Traits apply(Traits traits, Object[] objects) {
                return traits.andThen(new IsPartialFreemiumBassDiffusionModel() {
                    @Getter
                    @Setter
                    private ModelView rootModel;
                    private final String SDModelName = "Freemium Bass Diffusion Model";
                    private final String adopterAssetName = "Adopter";
                    private final AssetType adopterAssetType = new AssetType(adopterAssetName);
                    private final String AdopterType = "Adopter Type";
                    private final Qualifier PotentialAdopter = new Qualifier("Potential " + adopterAssetName);
                    private final Qualifier FreeAdopter = new Qualifier("Free " + adopterAssetName);
                    private final Qualifier PayingAdopter = new Qualifier("Paying " + adopterAssetName);
                    private final Qualifier CurrentAdopter = new Qualifier("Current " + adopterAssetName);

                    private Agent self;

                    private Agent getSelf(Engine engine, double oneDay, double dt) {
                        if (self != null) {
                            return self;
                        }
                        self = new Agent(engine);
                        SDDiagram<Double> sd = self.addSDDiagram(SDModelName, dt);
                        final double Month = oneDay * 30;
                        final String FirstTimeAdopters = "First Time Adopters";
                        final String TotalPopulation = "Total Population";
                        final String RWomPM = "WOM Conversions Per Month";
                        final String PConvincedByAdPM = "P(Convinced by Ad) Per Month";
                        final String PercentWhoBecomePayingAdopters = "Percent People Who Become Paying Adopters";
                        final String PercentWhoBecomeFreeAdopters = "Percent People Who Become Free Adopters";
                        final String AverageAdopterLifespanInMonths = "Average Adopter Lifespan in Months";
                        final String PercentFirstTimeUsersRetained = "Percent First Time Users Retained as Either Free or Paying";
                        sd
                                // General flow: [potential] ==> [first timer] ==> [free or paid] ==> (age out)
                                .addStock(FirstTimeAdopters, 0d)
                                .addStock(PotentialAdopter.getLabel(), (double) traits.lazyAs(HasPotentialAdopters.class).getPotentialAdopters())
                                .addStock(FreeAdopter.getLabel(), (double) traits.lazyAs(HasFreeAdopters.class, () -> () -> 0d).getFreeAdopters())
                                .addStock(PayingAdopter.getLabel(), (double) traits.lazyAs(HasPayingAdopters.class, () -> () -> 0d).getPayingAdopters())
                                .addVariable(CurrentAdopter.getLabel(), () -> sd.get(FreeAdopter.getLabel()) + sd.get(PayingAdopter.getLabel()))
                                .addVariable(TotalPopulation, () -> sd.get(CurrentAdopter.getLabel()) + sd.get(PotentialAdopter.getLabel()))

                                // Key adoption parameters: WOM and Ads
                                // Rates assumed to be in per-month figures for ease of use.
                                .addVariable(PConvincedByAdPM, () -> traits.lazyAs(HasConversionFromAds.class).getConversionPercentPerTimeUnit()/100.0 / Month)
                                .addVariable(RWomPM, () -> traits.lazyAs(HasSpreadThroughContacts.class).getConversionsPerTimeUnit() / Month)

                                // Variables:
                                // Retention of first timers, average lifespan of retained users, fractoin going to free or paid.
                                .addVariable(PercentFirstTimeUsersRetained,
                                        () -> traits.lazyAs(HasAdopterRetention.class, () -> () -> 100d)
                                                .getPercentFirstTimeUsersRetained())
                                .addVariable(AverageAdopterLifespanInMonths,
                                        () -> traits.lazyAs(HasAdopterLifespan.class, () -> () -> Double.POSITIVE_INFINITY)
                                                .getAverageMonthsThatAdoptersUseProduct())
                                .addVariable(PercentWhoBecomeFreeAdopters,
                                        () -> sd.get(PercentFirstTimeUsersRetained) * (100 - traits.lazyAs(HasPercentChanceOfUsersBecomingPayingUsers.class, () -> () -> 100d).getPercentChanceOfBecomingPayingUser())/100.0)
                                .addVariable(PercentWhoBecomePayingAdopters,
                                        () -> sd.get(PercentFirstTimeUsersRetained) * traits.lazyAs(HasPercentChanceOfUsersBecomingPayingUsers.class, () -> () -> 100d).getPercentChanceOfBecomingPayingUser()/100.0)

                                // [potential] ==> [first timers] @ (wom+ads)
                                .addFlow("First Time Adoption")
                                .from(PotentialAdopter.getLabel())
                                .to(FirstTimeAdopters)
                                .at((potentialAdopters, firstTimers) -> {
                                    // Adoption from WOM
                                    double c = sd.get(CurrentAdopter.getLabel());
                                    double t = sd.get(TotalPopulation);
                                    double wom = sd.get(CurrentAdopter.getLabel())
                                            * potentialAdopters / sd.get(TotalPopulation)
                                            * sd.get(RWomPM);
                                    // Adoptions from ads.
                                    double ads = potentialAdopters
                                            * sd.get(PConvincedByAdPM);
                                    Log.i("Model", String.format("%s total adopters, %s potential remaining", c, potentialAdopters));
                                    return wom + ads;
                                })

                                // [first timers] ==> [free user] @ ((1-P(become paid user)) / dt)
                                .addFlow("Committed Free User")
                                .from(FirstTimeAdopters)
                                .to(FreeAdopter.getLabel())
                                .at((firstTimers, a) -> {
                                    double p = sd.get(PercentWhoBecomeFreeAdopters);
                                    double result = sd.get(PercentWhoBecomeFreeAdopters) / 100.0 * firstTimers;
                                    return result;
                                })

                                // [first timers] ==> [paying user] @ ((1-P(become paid user)) / dt)
                                .addFlow("Committed Paying User")
                                .from(FirstTimeAdopters)
                                .to(PayingAdopter.getLabel())
                                .at((firstTimers, a) -> {
                                    double p = sd.get(PercentWhoBecomePayingAdopters);
                                    double result = sd.get(PercentWhoBecomePayingAdopters) / 100.0 * firstTimers;
                                    return result;
                                })

                                // [first timers] ==> [quit]
                                .addFlow("First Timers Quit")
                                .from(FirstTimeAdopters)
                                .toVoid()
                                .at((firstTimers, na) -> {
                                    double result = (100 - sd.get(PercentFirstTimeUsersRetained))/100.0 * firstTimers;
                                    return result;
                                })

                                // [paying user] ==> [age out]
                                .addFlow("Paying Adopters Quit")
                                .from(PayingAdopter.getLabel())
                                .toVoid()
                                .at((payingAdopters, na) -> {
                                    double result = payingAdopters/(sd.get(AverageAdopterLifespanInMonths) * Month);
                                    return result;
                                })

                                // [free user] ==> [age out]
                                .addFlow("Free Adopters Quit")
                                .from(FreeAdopter.getLabel())
                                .toVoid()
                                .at((freeAdopters, na) -> {
                                    double result = freeAdopters/(sd.get(AverageAdopterLifespanInMonths) * Month);
                                    return result;
                                });

                        self.start();
                        return self;
                    }

                    @Override
                    public Model get() {
                        return new Model() {@Override
                        public Set<AssetType> getAssetTypes() {
                            return new HashSet<>(Arrays.asList(adopterAssetType));
                        }

                            @Override
                            public Map<String, Set<Qualifier>> getAssetQualifiers(@NonNull AssetType assetType) {
                                if (adopterAssetType.equals(assetType)) {
                                    Map<String, Set<Qualifier>> qualifiers = new HashMap<>();
                                    qualifiers.put(AdopterType, new HashSet<>(Arrays.asList(FreeAdopter, PayingAdopter)));
                                    return qualifiers;
                                }
                                else {
                                    return Collections.emptyMap();
                                }
                            }

                            @Override
                            public double getCount(@NonNull AssetType assetType, @NonNull Map<String, Set<Qualifier>> qualifiers) {
                                if (self == null) {
                                    return 0;
                                }
                                if (adopterAssetType.equals(assetType)) {
                                    return qualifiers
                                            .getOrDefault(AdopterType, new HashSet<>(Arrays.asList(PayingAdopter, FreeAdopter)))
                                            .stream()
                                            .mapToDouble(q ->  (Double)self.getNumericSDDiagram(SDModelName).get(q.getLabel()))
                                            .sum();
                                }
                                else {
                                    return 0;
                                }
                            }

                            @Override
                            public Collection<Agent> asAgents(@NonNull Engine engine, double dayLength, double sdDiagramDt) {
                                return Arrays.asList(getSelf(engine, dayLength, sdDiagramDt));
                            }
                        };
                    }
                });
            }
        };
    }
}
