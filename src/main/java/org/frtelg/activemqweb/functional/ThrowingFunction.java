package org.frtelg.activemqweb.functional;

import java.util.function.Function;

@FunctionalInterface
public interface ThrowingFunction<I, O> {
    O apply(I i) throws Exception;

    static <In, Out> Function<In, Out> wrapCheckedException(ThrowingFunction<In, Out> function) {
        return (input) -> {
            try {
                return function.apply(input);
            } catch (Exception e) {
                throw new ThrowingFunctionException(e);
            }
        };
    }

    class ThrowingFunctionException extends RuntimeException {
        ThrowingFunctionException(Exception e) {
            super(e);
        }
    }
}
