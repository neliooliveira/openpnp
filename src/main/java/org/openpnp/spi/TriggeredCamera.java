/*
 * Copyright (C) 2026 OpenPnP contributors
 *
 * This file is part of OpenPnP.
 *
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 */
package org.openpnp.spi;

import java.awt.image.BufferedImage;

/**
 * Optional camera capability for deterministic hardware-triggered acquisition.
 *
 * This interface is intentionally separate from {@link Camera} so existing camera implementations
 * keep their current behaviour. Fly-by vision can detect this capability at runtime and fall back
 * to conventional stationary bottom vision when it is not available.
 */
public interface TriggeredCamera extends Camera {
    public enum TriggerMode {
        /** Normal continuous/live operation. */
        Live,
        /** One image is acquired for each external hardware trigger. */
        External
    }

    /**
     * A frame captured as the result of a hardware trigger.
     *
     * Sequence is monotonically increasing for the lifetime of the camera connection and is the
     * primary mechanism used to associate a requested fly-by shot with the returned image.
     * Timestamp is optional camera/driver timing information expressed in nanoseconds. A value of
     * zero means that no hardware timestamp is available.
     */
    public static class TriggeredFrame {
        private final BufferedImage image;
        private final long sequence;
        private final long timestampNanos;

        public TriggeredFrame(BufferedImage image, long sequence, long timestampNanos) {
            if (image == null) {
                throw new IllegalArgumentException("image must not be null");
            }
            this.image = image;
            this.sequence = sequence;
            this.timestampNanos = timestampNanos;
        }

        public BufferedImage getImage() {
            return image;
        }

        public long getSequence() {
            return sequence;
        }

        public long getTimestampNanos() {
            return timestampNanos;
        }
    }

    /**
     * Switch between normal live acquisition and external hardware-trigger mode.
     */
    void setTriggerMode(TriggerMode mode) throws Exception;

    /**
     * Returns the current trigger mode.
     */
    TriggerMode getTriggerMode();

    /**
     * Returns the most recently delivered trigger sequence, or -1 if none has been delivered.
     */
    long getLastTriggerSequence();

    /**
     * Wait for and return the next frame produced by an external hardware trigger.
     *
     * @param timeoutMilliseconds maximum wait time in milliseconds
     * @throws Exception if acquisition fails or the timeout expires
     */
    TriggeredFrame captureTriggered(long timeoutMilliseconds) throws Exception;
}
