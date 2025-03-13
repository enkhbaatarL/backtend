package com.sk.common.utils;

import java.util.Comparator;
import java.util.function.Function;

public class SortUtil {

    /**
     * Sort Comparator
     * - 숫자 -> 알파벳 -> 한글
     * - null 마지막 정렬
     */
    public static <T> Comparator<T> naturalComparator(Function<T, String> keyExtractor) {
        return Comparator.comparing(
            keyExtractor,
            Comparator.nullsLast(Comparator.naturalOrder())
        );
    }

}
