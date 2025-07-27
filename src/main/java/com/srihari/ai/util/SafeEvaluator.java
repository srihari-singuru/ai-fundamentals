package com.srihari.ai.util;

import java.util.function.Supplier;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SafeEvaluator {

    public static <T> T eval(Supplier<T> supplier, T fallback) {
        try {
            return supplier.get();
        } catch (Exception e) {
            log.warn("Safe evaluation failed, using fallback", e);
            return fallback;
        }
    }
}