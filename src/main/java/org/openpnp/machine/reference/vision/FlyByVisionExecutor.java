/*
 * Copyright (C) 2026 OpenPnP contributors
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
    private final FlyByVisionManager visionManager;

    public FlyByVisionExecutor() {
        this(FlyByVisionManager.get());
    }

    FlyByVisionExecutor(FlyByVisionManager visionManager) {
        this.visionManager = visionManager;
    }

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
            restoreLive(camera, driver);
            throw e;
        }
    }

    public void configureTiming(FlyByTriggerDriver driver, int cameraPulseMicroseconds,
            int ledStrobeMicroseconds) throws Exception {
        driver.setFlyByTiming(cameraPulseMicroseconds, ledStrobeMicroseconds);
    }

    /**
     * Complete acquisition from the camera. The frame sequence is the authoritative proof that the
     * external trigger occurred. Firmware M952 status is intentionally not required here because the
     * exact textual status response is not part of the stable M950-M953 wire contract yet.
     */
    public CaptureResult complete(TriggeredCamera camera, FlyByTriggerDriver driver,
            CaptureRequest request, long timeoutMilliseconds) throws Exception {
        try {
            CaptureResult result = visionManager.captureNext(camera, timeoutMilliseconds);
            if (result.getRequest().getRequestId() != request.getRequestId()) {
                throw new IllegalStateException("Fly-by request mismatch. Expected " + request.getRequestId()
                        + " but received " + result.getRequest().getRequestId() + ".");
            }
            return result;
        }
        finally {
            restoreLive(camera, driver);
        }
    }

    public void cancel(TriggeredCamera camera, FlyByTriggerDriver driver, CaptureRequest request)
            throws Exception {
        visionManager.cancel(camera);
        try {
            if (request != null) {
                driver.cancelFlyByTrigger(request.getRequestId());
            }
        }
        finally {
            restoreLive(camera, driver);
        }
    }

    private void restoreLive(TriggeredCamera camera, FlyByTriggerDriver driver) throws Exception {
        Exception failure = null;
        try {
            driver.setFlyByMode(FlyByMode.Live);
        }
        catch (Exception e) {
            failure = e;
        }
        try {
            visionManager.enterLiveMode(camera);
        }
        catch (Exception e) {
            if (failure == null) {
                failure = e;
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    public static boolean isSupported(Object camera, Object driver) {
        return camera instanceof TriggeredCamera && driver instanceof FlyByTriggerDriver;
    }
}
