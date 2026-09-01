/*
 * Copyright (C) 2026 OpenPnP contributors
 *
 * This file is part of OpenPnP.
 *
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 */
package org.openpnp.machine.reference.vision;

import org.openpnp.machine.reference.vision.FlyByVisionManager.CaptureRequest;
import org.openpnp.machine.reference.vision.FlyByVisionManager.CaptureResult;
import org.openpnp.model.Location;
import org.openpnp.spi.FlyByTriggerDriver;
import org.openpnp.spi.FlyByTriggerDriver.TriggerRequest;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.TriggeredCamera;

/**
 * Coordinates the OpenPnP side of one deterministic fly-by acquisition.
 *
 * Motion itself remains owned by the normal OpenPnP motion planner. This executor only arms the
 * camera association and the controller trigger before that motion, then receives and validates the
 * resulting frame afterwards. Keeping motion planning out of this class is important: acceleration,
 * blending and nozzle transforms remain exactly where OpenPnP already handles them.
 */
public class FlyByVisionExecutor {
    public static final int DEFAULT_TRIGGER_PULSE_MICROSECONDS = 50;
    public static final long DEFAULT_CAPTURE_TIMEOUT_MILLISECONDS = 1000;

    private final FlyByVisionManager visionManager;

    public FlyByVisionExecutor() {
        this(FlyByVisionManager.get());
    }

    FlyByVisionExecutor(FlyByVisionManager visionManager) {
        this.visionManager = visionManager;
    }

    /**
     * Arm camera and motion controller for one fly-by pass.
     *
     * @return the capture request whose id is also used as the controller trigger id.
     */
    public CaptureRequest arm(TriggeredCamera camera, FlyByTriggerDriver driver, Nozzle nozzle,
            Location triggerLocation, int pulseWidthMicroseconds) throws Exception {
        CaptureRequest captureRequest = visionManager.arm(camera, nozzle);
        try {
            TriggerRequest triggerRequest = new TriggerRequest(captureRequest.getRequestId(),
                    triggerLocation, pulseWidthMicroseconds);
            driver.armFlyByTrigger(nozzle, triggerRequest);
            return captureRequest;
        }
        catch (Exception e) {
            visionManager.cancel(camera);
            throw e;
        }
    }

    public CaptureRequest arm(TriggeredCamera camera, FlyByTriggerDriver driver, Nozzle nozzle,
            Location triggerLocation) throws Exception {
        return arm(camera, driver, nozzle, triggerLocation, DEFAULT_TRIGGER_PULSE_MICROSECONDS);
    }

    /**
     * Receive the frame produced by the armed pass and verify that the controller agrees the trigger
     * actually fired. This prevents a buffered camera frame from being accepted after a motion that
     * never crossed the trigger point.
     */
    public CaptureResult complete(TriggeredCamera camera, FlyByTriggerDriver driver,
            CaptureRequest request, long timeoutMilliseconds) throws Exception {
        CaptureResult result = visionManager.captureNext(camera, timeoutMilliseconds);
        if (result.getRequest().getRequestId() != request.getRequestId()) {
            throw new IllegalStateException("Fly-by request mismatch. Expected " + request.getRequestId()
                    + " but received " + result.getRequest().getRequestId() + ".");
        }
        if (!driver.hasFlyByTriggerFired(request.getRequestId())) {
            throw new IllegalStateException("Motion controller did not report fly-by trigger "
                    + request.getRequestId() + " as fired.");
        }
        return result;
    }

    public CaptureResult complete(TriggeredCamera camera, FlyByTriggerDriver driver,
            CaptureRequest request) throws Exception {
        return complete(camera, driver, request, DEFAULT_CAPTURE_TIMEOUT_MILLISECONDS);
    }

    /**
     * Best-effort cleanup for aborted or failed fly-by passes.
     */
    public void cancel(TriggeredCamera camera, FlyByTriggerDriver driver, CaptureRequest request)
            throws Exception {
        visionManager.cancel(camera);
        if (request != null) {
            driver.cancelFlyByTrigger(request.getRequestId());
        }
    }

    /**
     * Convenience check used before selecting Fly-By in Auto mode.
     */
    public static boolean isSupported(Object camera, Object driver) {
        return camera instanceof TriggeredCamera && driver instanceof FlyByTriggerDriver;
    }
}
