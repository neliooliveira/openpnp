/*
 * Copyright (C) 2026 OpenPnP contributors
 *
 * This file is part of OpenPnP.
 *
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 */
package org.openpnp.machine.reference.driver;

import java.util.Locale;

import org.openpnp.spi.FlyByTriggerDriver;
import org.openpnp.spi.HeadMountable;
import org.simpleframework.xml.Root;

/**
 * G-code driver extension for the Smoothieware-CHMT fly-by protocol.
 *
 * Protocol shared with Smoothieware-CHMT feature/flyby-vision:
 *   M950 S0|1|2                         mode LIVE/TRIGGER/AUTO
 *   M951 I<id> N<nozzle> D<mm> C0|1 L0|1   arm next motion
 *   M952                                status
 *   M953 C<camera-us> L<strobe-us>      pulse timing
 */
@Root
public class SmoothiewareChmtGcodeDriver extends GcodeDriver implements FlyByTriggerDriver {
    private static final int M_MODE = 950;
    private static final int M_ARM = 951;
    private static final int M_STATUS = 952;
    private static final int M_TIMING = 953;

    @Override
    public void setFlyByMode(FlyByMode mode) throws Exception {
        int value;
        switch (mode) {
            case Live:
                value = 0;
                break;
            case Trigger:
                value = 1;
                break;
            case Auto:
                value = 2;
                break;
            default:
                throw new IllegalArgumentException("Unsupported fly-by mode " + mode);
        }
        sendCommand(String.format(Locale.US, "M%d S%d", M_MODE, value));
    }

    @Override
    public void setFlyByTiming(int cameraPulseMicroseconds, int ledStrobeMicroseconds) throws Exception {
        if (cameraPulseMicroseconds < 0 || ledStrobeMicroseconds < 0) {
            throw new IllegalArgumentException("Fly-by timing values must not be negative.");
        }
        sendCommand(String.format(Locale.US, "M%d C%d L%d", M_TIMING,
                cameraPulseMicroseconds, ledStrobeMicroseconds));
    }

    @Override
    public void armFlyByTrigger(HeadMountable mountable, TriggerRequest request) throws Exception {
        String command = String.format(Locale.US,
                "M%d I%d N%d D%.6f C%d L%d",
                M_ARM,
                request.getRequestId(),
                request.getNozzleId(),
                request.getTriggerDistanceMillimeters(),
                request.isCameraTrigger() ? 1 : 0,
                request.isLedStrobe() ? 1 : 0);
        sendCommand(command);
    }

    @Override
    public void cancelFlyByTrigger(long requestId) throws Exception {
        // The firmware protocol does not define a dedicated cancel command. Returning to LIVE clears
        // the armed fly-by state safely; the next fly-by operation will explicitly select its mode.
        setFlyByMode(FlyByMode.Live);
    }

    @Override
    public boolean hasFlyByTriggerFired(long requestId) throws Exception {
        sendCommand(String.format(Locale.US, "M%d", M_STATUS));

        // Keep the host parser tolerant while the firmware status text is still evolving. The only
        // accepted positive result must contain both the requested id and an explicit fired marker.
        String response = receiveSingleResponse("^.*(?:FLYBY|flyby).*(?:I|id[=: ]+)" + requestId + ".*$");
        if (response == null) {
            return false;
        }
        String normalized = response.toLowerCase(Locale.US);
        return normalized.contains("fired=1")
                || normalized.contains("fired:1")
                || normalized.contains(" fired 1")
                || normalized.contains(" f1")
                || normalized.contains("triggered=1")
                || normalized.contains("triggered:1");
    }
}
