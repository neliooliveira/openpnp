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

/** Optional driver capability for deterministic motion-synchronized camera triggering. */
public interface FlyByTriggerDriver extends Driver {
    public enum FlyByMode { Live, Trigger, Auto }

    public static class TriggerRequest {
        private final long requestId;
        private final int nozzleId;
        private final double triggerDistanceMillimeters;
        private final boolean cameraTrigger;
        private final boolean ledStrobe;

        public TriggerRequest(long requestId, int nozzleId, double triggerDistanceMillimeters,
                boolean cameraTrigger, boolean ledStrobe) {
            if (requestId < 0 || nozzleId < 0 || triggerDistanceMillimeters < 0) {
                throw new IllegalArgumentException("Invalid fly-by trigger request");
            }
            this.requestId = requestId;
            this.nozzleId = nozzleId;
            this.triggerDistanceMillimeters = triggerDistanceMillimeters;
            this.cameraTrigger = cameraTrigger;
            this.ledStrobe = ledStrobe;
        }

        public long getRequestId() { return requestId; }
        public int getNozzleId() { return nozzleId; }
        public double getTriggerDistanceMillimeters() { return triggerDistanceMillimeters; }
        public boolean isCameraTrigger() { return cameraTrigger; }
        public boolean isLedStrobe() { return ledStrobe; }
    }

    void setFlyByMode(FlyByMode mode) throws Exception;
    void setFlyByTiming(int cameraPulseMicroseconds, int ledStrobeMicroseconds) throws Exception;
    void armFlyByTrigger(HeadMountable mountable, TriggerRequest request) throws Exception;
    void cancelFlyByTrigger(long requestId) throws Exception;
    boolean hasFlyByTriggerFired(long requestId) throws Exception;
}
