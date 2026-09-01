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
 * Dedicated processing pool for Fly-By vision frames.
 *
 * CvPipeline instances are stateful, therefore callers must submit one independent/cloned pipeline
 * per task. The pool parallelizes different frames while each individual pipeline remains ordered
 * stage-by-stage as required by CvPipeline semantics.
 *
 * The default parallelism is detected automatically from Runtime.availableProcessors() and may use
 * all logical processors when enough independent frame-processing work is queued.
 */
public final class FlyByVisionProcessingExecutor implements AutoCloseable {
    private static final FlyByVisionProcessingExecutor instance =
            new FlyByVisionProcessingExecutor(defaultParallelism());

    public static FlyByVisionProcessingExecutor get() {
        return instance;
    }

    private static int defaultParallelism() {
        return Math.max(1, Runtime.getRuntime().availableProcessors());
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

    /** Number of logical processors available to Fly-By frame processing. */
    public int getParallelism() {
        return parallelism;
    }

    /**
     * Submit processing for one already-captured frame. Threads are created on demand by the fixed
     * pool and work is distributed automatically up to the detected processor count. Callers must
     * not reuse the same CvPipeline object concurrently between tasks.
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
