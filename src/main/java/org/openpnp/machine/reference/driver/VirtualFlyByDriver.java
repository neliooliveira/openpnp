/*
 * Copyright (C) 2026 OpenPnP contributors
 */
package org.openpnp.machine.reference.driver;

import org.openpnp.model.Motion.MoveToCommand;
import org.openpnp.spi.FlyByTriggerDriver;
import org.openpnp.spi.HeadMountable;
import org.simpleframework.xml.Root;

/**
 * NullDriver with the Fly-By trigger protocol implemented entirely in memory.
 *
 * This allows complete OpenPnP Fly-By configuration and job testing without a controller or camera.
 */
@Root
public class VirtualFlyByDriver extends NullDriver implements FlyByTriggerDriver {
    private volatile FlyByMode flyByMode = FlyByMode.Live;
    private volatile TriggerRequest armedRequest;
    private volatile long firedRequestId = -1;
    private volatile int cameraPulseMicroseconds = 1000;
    private volatile int ledStrobeMicroseconds = 100;

    @Override
    public void setFlyByMode(FlyByMode mode) throws Exception {
        if (mode == null) {
            throw new IllegalArgumentException("mode must not be null");
        }
        flyByMode = mode;
        if (mode == FlyByMode.Live) {
            armedRequest = null;
            firedRequestId = -1;
        }
    }

    @Override
    public void setFlyByTiming(int cameraPulseMicroseconds, int ledStrobeMicroseconds)
            throws Exception {
        if (cameraPulseMicroseconds < 0 || ledStrobeMicroseconds < 0) {
            throw new IllegalArgumentException("Fly-By timing values must not be negative.");
        }
        this.cameraPulseMicroseconds = cameraPulseMicroseconds;
        this.ledStrobeMicroseconds = ledStrobeMicroseconds;
    }

    @Override
    public void armFlyByTrigger(HeadMountable mountable, TriggerRequest request) throws Exception {
        if (flyByMode == FlyByMode.Live) {
            throw new IllegalStateException("Virtual Fly-By driver is in live mode.");
        }
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        armedRequest = request;
        firedRequestId = -1;
    }

    @Override
    public void moveTo(HeadMountable mountable, MoveToCommand move) throws Exception {
        super.moveTo(mountable, move);
        TriggerRequest request = armedRequest;
        if (request != null && flyByMode != FlyByMode.Live) {
            firedRequestId = request.getRequestId();
            armedRequest = null;
        }
    }

    @Override
    public void cancelFlyByTrigger(long requestId) throws Exception {
        TriggerRequest request = armedRequest;
        if (request != null && request.getRequestId() == requestId) {
            armedRequest = null;
        }
        if (firedRequestId == requestId) {
            firedRequestId = -1;
        }
        flyByMode = FlyByMode.Live;
    }

    @Override
    public boolean hasFlyByTriggerFired(long requestId) throws Exception {
        return firedRequestId == requestId;
    }

    public FlyByMode getFlyByMode() {
        return flyByMode;
    }

    public int getCameraPulseMicroseconds() {
        return cameraPulseMicroseconds;
    }

    public int getLedStrobeMicroseconds() {
        return ledStrobeMicroseconds;
    }
}
