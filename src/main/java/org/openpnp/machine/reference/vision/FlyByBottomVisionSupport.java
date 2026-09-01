/*
 * Copyright (C) 2026 OpenPnP contributors
 */
package org.openpnp.machine.reference.vision;

import org.openpnp.machine.reference.camera.BufferedImageCamera;
import org.openpnp.model.BottomVisionSettings;
import org.openpnp.model.BottomVisionSettings.AcquisitionMode;
import org.openpnp.spi.Camera;
import org.openpnp.spi.FlyByTriggerDriver;
import org.openpnp.spi.TriggeredCamera;

/**
 * Shared Fly-By bottom-vision integration helpers.
 *
 * This class deliberately keeps the triggered acquisition separate from ReferenceBottomVision's
 * existing alignment math. Once a triggered frame has been captured, the standard CvPipeline can
 * process it through a BufferedImageCamera, preserving the real camera calibration while avoiding a
 * second hardware capture.
 */
public final class FlyByBottomVisionSupport {
    private FlyByBottomVisionSupport() {
    }

    /**
     * Resolve whether Fly-By should be used for the current settings and capabilities.
     */
    public static boolean shouldUseFlyBy(BottomVisionSettings settings, Camera camera,
            Object driver) {
        if (settings == null || camera == null || driver == null) {
            return false;
        }
        AcquisitionMode mode = settings.getAcquisitionMode();
        if (mode == AcquisitionMode.Stationary) {
            return false;
        }
        boolean supported = camera instanceof TriggeredCamera && driver instanceof FlyByTriggerDriver;
        if (mode == AcquisitionMode.FlyBy) {
            return supported;
        }
        return supported;
    }

    /**
     * Convert one triggered frame into a normal Camera for the existing CvPipeline.
     *
     * BufferedImageCamera copies units-per-pixel from the real camera, so all current bottom-vision
     * pixel-to-machine-coordinate calculations remain unchanged.
     */
    public static Camera createPipelineCamera(Camera realCamera,
            FlyByVisionManager.CaptureResult captureResult) {
        if (realCamera == null || captureResult == null || captureResult.getFrame() == null) {
            throw new IllegalArgumentException("camera and captureResult must not be null");
        }
        return BufferedImageCamera.get(realCamera, captureResult.getFrame().getImage());
    }

    /**
     * Validate a configured Fly-By request before motion starts.
     */
    public static void validate(BottomVisionSettings settings, Camera camera, Object driver)
            throws Exception {
        if (settings == null) {
            throw new IllegalArgumentException("settings must not be null");
        }
        if (settings.getAcquisitionMode() == AcquisitionMode.Stationary) {
            return;
        }
        if (!(camera instanceof TriggeredCamera) || !(driver instanceof FlyByTriggerDriver)) {
            if (settings.getAcquisitionMode() == AcquisitionMode.FlyBy
                    && !settings.isFlyByFallbackToStationary()) {
                throw new Exception("Fly-By bottom vision requires a TriggeredCamera and "
                        + "FlyByTriggerDriver.");
            }
        }
        if (settings.getFlyByApproachDistanceMm() <= 0
                || !Double.isFinite(settings.getFlyByApproachDistanceMm())) {
            throw new Exception("Fly-By approach distance must be a finite positive value.");
        }
        if (settings.getFlyByCameraPulseMicroseconds() <= 0) {
            throw new Exception("Fly-By camera trigger pulse must be positive.");
        }
        if (settings.getFlyByCaptureTimeoutMilliseconds() <= 0) {
            throw new Exception("Fly-By capture timeout must be positive.");
        }
    }
}
