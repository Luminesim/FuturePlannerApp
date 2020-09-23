package com.luminesim.futureplanner.models;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * A model template. Allows a model to be created with standard asset types, dimensions,
 * parameters, etc.
 */
@Getter
public class ModelTemplate {
    private final String name;

    private final Map<String, AssetDefinition> types;
    /**
     * Monad IDs defining how the model will be set up.
     */
    private final List<SetupInstructions> setupInstructions;

    public ModelTemplate(@NonNull String modelName, @NonNull String genericAssetNameInModel, @NonNull AssetDefinition templateAssetDefinition, @NonNull List<SetupInstructions> setupInstructions) {
        this.name = modelName;
        this.types = new HashMap<>();
        this.types.put(genericAssetNameInModel, templateAssetDefinition);
        if (!templateAssetDefinition.getQualifierOptions().isEmpty()) {

            // Sanity check: Each combination of qualifiers must have setup instructions.
            //TODO: Ensure there is setup instructions for each qualifier.
        }
        this.setupInstructions = setupInstructions;
    }

    @Data
    @AllArgsConstructor
    public static class AssetDefinition {
        private final AssetType type;

        /**
         * The qualifier options available for each qualifier.
         * E.g. Gender: Male, Female
         */
        private final Map<String, Set<Qualifier>> qualifierOptions;

        public AssetDefinition(String typeName) {
            this(new AssetType(typeName), new HashMap<>());
        }
    }

    @Getter
    public static class SetupStep {
        private final String monadId;
        private final Object[] monadParameters;

        public SetupStep(@NonNull String monadId, Object... params) {
            this.monadId = monadId;
            this.monadParameters = params;
        }
    }

    @Data
    public static class SetupInstructions {
        private final Map<String, Qualifier> qualifiers;
        private final List<SetupStep> steps;

        public SetupInstructions(Map<String, Qualifier> qualifiers, SetupStep... steps) {
            this.qualifiers = qualifiers;
            this.steps = Arrays.asList(steps);
        }

        public SetupInstructions(SetupStep... steps) {
            this(Collections.emptyMap(), steps);
        }
    }
}
