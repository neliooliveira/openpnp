/*
 * Copyright (C) 2026 OpenPnP contributors
 */
package org.openpnp.machine.reference.vision;

import org.openpnp.machine.reference.camera.BufferedImageCamera;
import org.openpnp.model.BottomVisionSettings;
import org.openpnp.model.BottomVisionSettings.AcquisitionMode;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Driver;
import org.openpnp.spi.FlyByTriggerDriver;
import org.openpnp.spi.Machine;
import org.openpnp.spi.Nozzle;
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
     * Find the Fly-By driver responsible for the nozzle axes. A single Fly-By driver is accepted
     * for machines without mapped axes as well, which is useful for virtual configurations.
     */
    public static FlyByTriggerDriver findDriver(Machine machine, Nozzle nozzle) {
        if (machine == null || nozzle == null) {
            return null;
        }
        FlyByTriggerDriver onlyCandidate = null;
        int candidateCount = 0;
        for (Driver driver : machine.getDrivers()) {
            if (driver instanceof FlyByTriggerDriver) {
                FlyByTriggerDriver candidate = (FlyByTriggerDriver) driver;
                if (!nozzle.getMappedAxes(machine).drivenBy(driver).isEmpty()) {
                    return candidate;
                }
                candidateCount++;
                if (candidateCount == 1) {
                    onlyCandidate = candidate;
                }
                else {
                    // Multiple unassociated candidates are ambiguous.
                    onlyCandidate = null;
                }
            }
        }
        return onlyCandidate;
    }

    /** Return the one-based nozzle number used by the controller protocol. */
    public static int getNozzleNumber(Nozzle nozzle) throws Exception {
        if (nozzle == null || nozzle.getHead() == null) {
            throw new Exception("Fly-By bottom vision requires a nozzle attached to a head.");
        }
        int nozzleIndex = nozzle.getHead().getNozzles().indexOf(nozzle);
        if (nozzleIndex < 0 || nozzleIndex >= 255) {
            throw new Exception("Fly-By nozzle is not registered on its head or exceeds protocol limits.");
        }
        return nozzleIndex + 1;
    }

    /**
     * Calculate a point that approaches the shot along the current XY travel direction. If the
     * nozzle is already at the shot XY, use the negative X direction deterministically.
     */
    public static Location getApproachLocation(Location currentLocation, Location shotLocation,
            double approachDistanceMillimeters) {
        Location current = currentLocation.convertToUnits(LengthUnit.Millimeters);
        Location shot = shotLocation.convertToUnits(LengthUnit.Millimeters);
        double dx = shot.getX() - current.getX();
        double dy = shot.getY() - current.getY();
        double travel = Math.hypot(dx, dy);
        if (travel == 0) {
            dx = 1;
            dy = 0;
            travel = 1;
        }
        Location approach = shot.derive(
                shot.getX() - dx / travel * approachDistanceMillimeters,
                shot.getY() - dy / travel * approachDistanceMillimeters, null, null);
        return approach.convertToUnits(shotLocation.getUnits());
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
            return;
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
