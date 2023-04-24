package org.frtelg.activemqweb.functional;

import java.util.function.Consumer;

@FunctionalInterface
public interface ThrowingConsumer<T> {
    void accept(T t) throws Exception;

    static <In> Consumer<In> wrapCheckedException(ThrowingConsumer<In> consumer) {
        return (value) -> {
            try {
                consumer.accept(value);
            } catch (Exception e) {
                throw new ThrowingConsumerException(e);
            }
        };
    }

    class ThrowingConsumerException extends RuntimeException {
        ThrowingConsumerException(Exception e) {
            super(e);
        }
    }
}
