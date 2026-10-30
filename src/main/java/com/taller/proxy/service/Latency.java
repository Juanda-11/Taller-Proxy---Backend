package com.taller.proxy.service;

import java.util.concurrent.ThreadLocalRandom;

/** Simula la latencia de red/procesamiento de un microservicio real. */
final class Latency {

    private Latency() {
    }

    static void simulate(int minMs, int maxMs) {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(minMs, maxMs + 1));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
