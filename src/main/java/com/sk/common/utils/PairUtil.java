package com.sk.common.utils;

public class PairUtil<T, U> {
    private final T first;
    private final U second;

    public PairUtil(T first, U second) {
        this.first = first;
        this.second = second;
    }

    public T getFirst() {
        return first;
    }

    public U getSecond() {
        return second;
    }
}