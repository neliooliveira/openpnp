/*
 * Copyright (C) 2026 OpenPnP contributors
 */
package org.openpnp.machine.reference.camera;

import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicLong;

import org.openpnp.spi.TriggeredCamera;

/**
 * Simulation camera for Fly-By vision.
 *
 * It reuses ImageCamera's existing OpenPnP simulation rendering, while exposing the same
 * TriggeredCamera capability expected from a real USB3 Vision / GenICam camera. This allows the
 * complete Fly-By acquisition path to be exercised without camera hardware.
 */
public class FlyByImageCamera extends ImageCamera implements TriggeredCamera {
    private final AtomicLong triggerSequence = new AtomicLong();
    private volatile TriggerMode triggerMode = TriggerMode.Live;

    @Override
    public void setTriggerMode(TriggerMode mode) throws Exception {
        if (mode == null) {
            throw new IllegalArgumentException("mode must not be null");
        }
        triggerMode = mode;
    }

    @Override
    public TriggerMode getTriggerMode() {
        return triggerMode;
    }

    @Override
    public long getLastTriggerSequence() {
        return triggerSequence.get();
    }

    @Override
    public TriggeredFrame captureTriggered(long timeoutMilliseconds) throws Exception {
        if (triggerMode != TriggerMode.External) {
            throw new IllegalStateException("FlyByImageCamera is not in external trigger mode.");
        }
        if (timeoutMilliseconds <= 0) {
            throw new IllegalArgumentException("timeoutMilliseconds must be positive");
        }

        BufferedImage image = internalCapture();
        if (image == null) {
            throw new IllegalStateException("Simulation camera did not produce an image.");
        }

        long sequence = triggerSequence.incrementAndGet();
        return new TriggeredFrame(image, sequence, System.nanoTime());
    }
}
