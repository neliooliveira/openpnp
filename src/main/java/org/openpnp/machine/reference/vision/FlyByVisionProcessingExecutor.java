/*
 * Copyright (C) 2026 OpenPnP contributors
 */
package org.openpnp.machine.reference.vision;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Dedicated bounded processing pool for Fly-By vision frames.
 *
 * CvPipeline instances are stateful, therefore callers must submit one independent/cloned pipeline
 * per task. The pool parallelizes different frames (for example nozzle 1 and nozzle 2), while each
 * individual pipeline remains ordered stage-by-stage as required by CvPipeline semantics.
 */
public final class FlyByVisionProcessingExecutor implements AutoCloseable {
    private static final FlyByVisionProcessingExecutor instance =
            new FlyByVisionProcessingExecutor(defaultParallelism());

    public static FlyByVisionProcessingExecutor get() {
        return instance;
    }

    private static int defaultParallelism() {
        int cores = Runtime.getRuntime().availableProcessors();
        // Two concurrent frames are enough for the current dual-nozzle CHMT design. Leave CPU headroom
        // for the GUI, motion planner and OpenCV's own native worker threads.
        return Math.max(1, Math.min(2, cores - 1));
    }

    private final int parallelism;
    private final ExecutorService executor;

    public FlyByVisionProcessingExecutor(int parallelism) {
        if (parallelism < 1) {
            throw new IllegalArgumentException("parallelism must be at least 1");
        }
        this.parallelism = parallelism;
        AtomicInteger threadNumber = new AtomicInteger();
        ThreadFactory factory = runnable -> {
            Thread thread = new Thread(runnable,
                    "OpenPnP-FlyByVision-" + threadNumber.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
        executor = Executors.newFixedThreadPool(parallelism, factory);
    }

    public int getParallelism() {
        return parallelism;
    }

    /**
     * Submit processing for one already-captured frame. Callers must not reuse the same CvPipeline
     * object concurrently between tasks.
     */
    public <T> Future<T> submit(Callable<T> processingTask) {
        if (processingTask == null) {
            throw new IllegalArgumentException("processingTask must not be null");
        }
        return executor.submit(processingTask);
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
