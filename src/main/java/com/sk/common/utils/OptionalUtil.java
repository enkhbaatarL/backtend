package com.sk.common.utils;

import java.util.Optional;
import java.util.function.Supplier;

public class OptionalUtil {

    public static <T> Optional<T> safelyAccess(Supplier<T> supplier) {
        try {
            return Optional.ofNullable(supplier.get());
        } catch (NullPointerException e) {
            return Optional.empty();
        }
    }

}
