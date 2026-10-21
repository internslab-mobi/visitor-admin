
package com.adminvisitor.auth.util;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Arrays;

class BCryptBenchmarkTest {

    private static final String PASSWORD = "BenchmarkPassword123!";

    @Test
    void benchmarkBcryptCosts() {
        int[] costs = {10, 11, 12, 13};

        // Warm up the JVM before collecting measurements.
        BCryptPasswordEncoder warmupEncoder =
                new BCryptPasswordEncoder(10);

        for (int i = 0; i < 3; i++) {
            String hash = warmupEncoder.encode(PASSWORD);
            warmupEncoder.matches(PASSWORD, hash);
        }

        System.out.println();
        System.out.println("========== BCrypt Benchmark ==========");
        System.out.printf(
                "%-10s %-18s %-22s%n",
                "Cost", "Hash (ms)", "Verification (ms)"
        );

        for (int cost : costs) {
            BCryptPasswordEncoder encoder =
                    new BCryptPasswordEncoder(cost);

            long[] hashTimes = new long[5];
            long[] verifyTimes = new long[5];

            // Prepare a hash outside the verification measurements.
            String existingHash = encoder.encode(PASSWORD);

            for (int i = 0; i < 5; i++) {
                long startHash = System.nanoTime();
                String hash = encoder.encode(PASSWORD);
                hashTimes[i] = System.nanoTime() - startHash;

                long startVerify = System.nanoTime();
                boolean matches = encoder.matches(
                        PASSWORD, existingHash
                );
                verifyTimes[i] = System.nanoTime() - startVerify;

                if (!matches) {
                    throw new AssertionError(
                            "Password verification failed at cost " + cost
                    );
                }
            }

            double averageHashMs = averageMs(hashTimes);
            double averageVerifyMs = averageMs(verifyTimes);

            System.out.printf(
                    "%-10d %-18.2f %-22.2f%n",
                    cost, averageHashMs, averageVerifyMs
            );
        }

        System.out.println("======================================");
    }

    private double averageMs(long[] times) {
        // Exclude the slowest and fastest runs to reduce outlier impact.
        long[] sorted = Arrays.copyOf(times, times.length);
        Arrays.sort(sorted);

        long sum = 0;
        for (int i = 1; i < sorted.length - 1; i++) {
            sum += sorted[i];
        }

        return sum / (double) (sorted.length - 2) / 1_000_000.0;
    }
}
