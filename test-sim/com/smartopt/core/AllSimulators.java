package com.smartopt.core;

/** Chạy tất cả các bộ mô phỏng logic thuần Java trong 1 lệnh duy nhất. */
public class AllSimulators {
    public static void main(String[] args) throws Exception {
        run("OptimizationEngine", EngineSimulator.class);
        run("RateLimiter", RateLimiterSimulator.class);
        run("ProtectionRules", ProtectionRulesSimulator.class);
        run("JoinRampCalculator", JoinRampSimulator.class);
        run("GradientUtil", com.smartopt.util.GradientUtilSimulator.class);
        run("HistoryFileWriter", HistoryFileWriterSimulator.class);
    }

    private static void run(String name, Class<?> cls) throws Exception {
        System.out.println("\n############################################");
        System.out.println("# CHAY MO PHONG: " + name);
        System.out.println("############################################");
        cls.getMethod("main", String[].class).invoke(null, (Object) new String[0]);
    }
}
