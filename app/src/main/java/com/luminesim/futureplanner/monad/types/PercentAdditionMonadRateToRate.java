package com.luminesim.futureplanner.monad.types;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import ca.anthrodynamics.indes.lang.Monad;
import ca.anthrodynamics.indes.lang.MonadInformation;
import ca.anthrodynamics.indes.lang.Rate;
import ca.anthrodynamics.indes.lang.ScheduledRate;
import lombok.NonNull;

public class PercentAdditionMonadRateToRate extends Monad<Rate, Rate> {

    public PercentAdditionMonadRateToRate(@NonNull String additionParamName) {
        super(
                getDefaultInfo(),
                rate -> !(rate.getOutType().get().equals(ScheduledRate.class)),
                new Class[]{Number.class},
                new String[]{additionParamName});
    }

    private static MonadInformation<Rate, Rate> getDefaultInfo() {
        Map<String, Object> properties = new HashMap<>();
        MonadInformation info = new MonadInformation(properties, Optional.of(Rate.class), Optional.of(Rate.class));
        return info;
    }

    /**
     * Returns the value with deduction applied.
     *
     * @param in
     * @return
     */
    @Override
    protected Rate apply(Rate in, Object[] params) {

        // HACK TODO FIXME
        Rate out = () -> in.getAsDouble() * (1 + (Double) params[0]/100.0);
//        if (in instanceof ScheduledRate) {
//            ScheduledRate realIn = (ScheduledRate)in;
//            return new ScheduledRate(out, realIn.getStart(), realIn.getEnd());
//        }
//        else {
            return out;
//        }
    }
}
