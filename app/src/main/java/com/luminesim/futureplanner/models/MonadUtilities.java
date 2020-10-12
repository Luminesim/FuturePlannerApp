package com.luminesim.futureplanner.models;

import java.util.function.Function;

import ca.anthrodynamics.indes.lang.Monad;
import ca.anthrodynamics.indes.lang.MonadInformation;
import ca.anthrodynamics.indes.lang.Traits;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MonadUtilities {

//    public static <A, B> Monad addNonReplaceableProperty(Class<A> in, Class<B> out, Function<Object[], B> logic) {
//        return new Monad<A,B>(in, out, options -> !options.getProperties().canDuckTypeAs(out)) {
//            @Override
//            protected Traits apply(Traits traits, Object[] objects) {
//                return traits.andThen(logic.apply(objects));
//            }
//        };
//    }

    public static <A, B> Monad addNonReplaceableProperty(Class<A> in, Class<B> out, String paramName, Class paramType, Function<Object[], B> logic) {
        return new Monad<A,B>(in, out, options -> !options.getProperties().canDuckTypeAs(out), paramType, paramName) {
            @Override
            protected Traits apply(Traits traits, Object[] objects) {
                return traits.andThen(logic.apply(objects));
            }
        };
    }
}
