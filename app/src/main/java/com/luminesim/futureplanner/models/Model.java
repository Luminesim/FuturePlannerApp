package com.luminesim.futureplanner.models;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import ca.anthrodynamics.indes.Engine;
import ca.anthrodynamics.indes.abm.Agent;
import lombok.NonNull;

/**
 * A model.
 */
public interface Model extends ModelView {
    /**
     *
     * @return The model encapsulated in an {@link Agent}.
     * Any changes to the agent that would be reflected in a {@link ModelView} should be reported
     * correctly. E.g. if the agent contains an SD diagram that sees customers grow from
     * 2 to 200, then requesting information from the Model on customers should report that
     * there are 200.
     */
    Collection<Agent> asAgents(@NonNull Engine engine, double sdDiagramDt);

    /**
     * @param root
     * Sets the root model, allowing for whole-of-simulation queries.
     */
    void setRootModel(@NonNull ModelView root);

    /**
     *
     * @pre all models report the same root model.
     * @return
     */
    public static Model compose(List<Model> models) {

        return new Model() {
            @Override
            public Set<AssetType> getAssetTypes() {
                return models.stream().flatMap(m -> m.getAssetTypes().stream()).collect(Collectors.toSet());
            }

            @Override
            public Map<String, Set<Qualifier>> getAssetQualifiers(@NonNull AssetType assetType) {
                Map<String, Set<Qualifier>> qualifiers = new HashMap<>();
                models.forEach(m -> m.getAssetQualifiers(assetType).forEach((qualifierGroup, qualifierItems) -> {
                    qualifiers.computeIfAbsent(qualifierGroup, x -> new HashSet<>());
                    qualifiers.get(qualifierGroup).addAll(qualifierItems);
                }));
                return qualifiers;
            }

            @Override
            public double getCount(@NonNull AssetType assetType, @NonNull Map<String, Set<Qualifier>> qualifiers) {
                return models.stream().mapToDouble(m -> m.getCount(assetType, qualifiers)).sum();
            }

            @Override
            public Collection<Agent> asAgents(@NonNull Engine engine, double sdDiagramDt) {
                return models.stream().map(m -> m.asAgents(engine, sdDiagramDt)).flatMap(Collection::stream).collect(Collectors.toList());
            }

            @Override
            public void setRootModel(@NonNull ModelView root) {
                models.forEach(m -> m.setRootModel(root));
            }

            @Override
            public ModelView getRootModel() {
                return models.stream().map(Model::getRootModel).findFirst().get();
            }


        };
    }
}
