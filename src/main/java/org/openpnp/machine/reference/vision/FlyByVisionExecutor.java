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
import org.openpnp.spi.FlyByTriggerDriver;
import org.openpnp.spi.FlyByTriggerDriver.FlyByMode;
import org.openpnp.spi.FlyByTriggerDriver.TriggerRequest;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.TriggeredCamera;

/** Coordinates one deterministic fly-by acquisition on the OpenPnP side. */
public class FlyByVisionExecutor {
    public static final int DEFAULT_CAMERA_PULSE_MICROSECONDS = 50;
    public static final int DEFAULT_LED_STROBE_MICROSECONDS = 50;
    public static final long DEFAULT_CAPTURE_TIMEOUT_MILLISECONDS = 1000;

    private final FlyByVisionManager visionManager;

    public FlyByVisionExecutor() {
        this(FlyByVisionManager.get());
    }

    FlyByVisionExecutor(FlyByVisionManager visionManager) {
        this.visionManager = visionManager;
    }

    /**
     * Prepare the camera and firmware for one trigger in the next motion.
     *
     * triggerDistanceMillimeters is measured from the beginning of the next firmware motion block,
     * matching Smoothieware-CHMT M951 D<mm> semantics.
     */
    public CaptureRequest arm(TriggeredCamera camera, FlyByTriggerDriver driver, Nozzle nozzle,
            int nozzleId, double triggerDistanceMillimeters, boolean ledStrobe) throws Exception {
        CaptureRequest captureRequest = visionManager.arm(camera, nozzle);
        try {
            driver.setFlyByMode(FlyByMode.Trigger);
            TriggerRequest triggerRequest = new TriggerRequest(captureRequest.getRequestId(), nozzleId,
                    triggerDistanceMillimeters, true, ledStrobe);
            driver.armFlyByTrigger(nozzle, triggerRequest);
            return captureRequest;
        }
        catch (Exception e) {
            visionManager.cancel(camera);
            try {
                driver.setFlyByMode(FlyByMode.Live);
            }
            catch (Exception ignored) {
            }
            throw e;
        }
    }

    public void configureTiming(FlyByTriggerDriver driver, int cameraPulseMicroseconds,
            int ledStrobeMicroseconds) throws Exception {
        driver.setFlyByTiming(cameraPulseMicroseconds, ledStrobeMicroseconds);
    }

    public CaptureResult complete(TriggeredCamera camera, FlyByTriggerDriver driver,
            CaptureRequest request, long timeoutMilliseconds) throws Exception {
        try {
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
        finally {
            driver.setFlyByMode(FlyByMode.Live);
        }
    }

    public CaptureResult complete(TriggeredCamera camera, FlyByTriggerDriver driver,
            CaptureRequest request) throws Exception {
        return complete(camera, driver, request, DEFAULT_CAPTURE_TIMEOUT_MILLISECONDS);
    }

    public void cancel(TriggeredCamera camera, FlyByTriggerDriver driver, CaptureRequest request)
            throws Exception {
        visionManager.cancel(camera);
        if (request != null) {
            driver.cancelFlyByTrigger(request.getRequestId());
        }
        else {
            driver.setFlyByMode(FlyByMode.Live);
        }
    }

    public static boolean isSupported(Object camera, Object driver) {
        return camera instanceof TriggeredCamera && driver instanceof FlyByTriggerDriver;
    }
}
