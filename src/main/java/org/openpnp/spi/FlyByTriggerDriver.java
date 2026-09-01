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

import org.openpnp.model.Location;

/**
 * Optional driver capability for deterministic position-synchronized camera triggering.
 *
 * Implementations arm the motion controller so that a hardware output pulse is generated when the
 * specified HeadMountable crosses the requested physical location during a subsequent move. The
 * trigger is consumed once fired unless the implementation explicitly documents otherwise.
 */
public interface FlyByTriggerDriver extends Driver {

    /**
     * Immutable trigger request passed to the motion controller.
     */
    public static class TriggerRequest {
        private final long requestId;
        private final Location triggerLocation;
        private final int pulseWidthMicroseconds;

        public TriggerRequest(long requestId, Location triggerLocation, int pulseWidthMicroseconds) {
            if (triggerLocation == null) {
                throw new IllegalArgumentException("triggerLocation must not be null");
            }
            if (pulseWidthMicroseconds <= 0) {
                throw new IllegalArgumentException("pulseWidthMicroseconds must be positive");
            }
            this.requestId = requestId;
            this.triggerLocation = triggerLocation;
            this.pulseWidthMicroseconds = pulseWidthMicroseconds;
        }

        public long getRequestId() {
            return requestId;
        }

        public Location getTriggerLocation() {
            return triggerLocation;
        }

        public int getPulseWidthMicroseconds() {
            return pulseWidthMicroseconds;
        }
    }

    /**
     * Arm one position-synchronized trigger for a future motion of the specified mountable.
     */
    void armFlyByTrigger(HeadMountable mountable, TriggerRequest request) throws Exception;

    /**
     * Cancel any not-yet-fired trigger associated with the specified request id.
     */
    void cancelFlyByTrigger(long requestId) throws Exception;

    /**
     * Return true when the controller reports that the specified trigger has fired.
     */
    boolean hasFlyByTriggerFired(long requestId) throws Exception;
}
