package com.srihari.ai.util;

import java.util.function.Supplier;

public class SafeEvaluator {
    public static <T> T eval(Supplier<T> supplier, T fallback) {
        try {
            return supplier.get();
        } catch (Exception e) {
            return fallback;
        }
    }
}