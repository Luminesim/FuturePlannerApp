package com.luminesim.futureplanner.purchases;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Builder
@Getter
@AllArgsConstructor
public class FeatureSet {
    @Builder.Default
    private boolean isAdvertisingEnabled = true;

    @Builder.Default
    private boolean isTwoYearRuntimeEnabled = false;

    @Builder.Default
    private boolean isFiveYearRuntimeEnabled = false;

    /**
     *
     * @param other
     * @return
     *  A feature set with attributes of both. If either disables ads, ads are disabled.
     */
    public FeatureSet and(@NonNull FeatureSet  other) {
        return new FeatureSet(
                // False is good
                (this.isAdvertisingEnabled && other.isAdvertisingEnabled),

                // True is good
                (this.isTwoYearRuntimeEnabled || other.isTwoYearRuntimeEnabled),

                // True is good
                (this.isFiveYearRuntimeEnabled || other.isFiveYearRuntimeEnabled)
        );
    }

    public  static FeatureSet noFeatures() {
        return FeatureSet.builder().build();
    }

    /**
     *
     * @param other
     * @return
     *  True, if the other feature set has any features in common with this feature set.
     *  E.g. both disable ads, both increase runtime to five years, etc.
     */
    public boolean overlaps(@NonNull FeatureSet other) {
        return (this.isAdvertisingEnabled == other.isAdvertisingEnabled
            || this.isTwoYearRuntimeEnabled == other.isTwoYearRuntimeEnabled
            || this.isFiveYearRuntimeEnabled == other.isFiveYearRuntimeEnabled);
    }
}
