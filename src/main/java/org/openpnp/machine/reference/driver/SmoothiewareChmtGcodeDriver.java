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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openpnp.model.Location;
import org.openpnp.spi.FlyByTriggerDriver;
import org.openpnp.spi.HeadMountable;
import org.simpleframework.xml.Root;

/**
 * G-code driver extension for the Smoothieware-CHMT firmware fork.
 *
 * Fly-by protocol:
 *   M990 S1 I<id> X<x> Y<y> P<pulse-us>  - arm one trigger
 *   M990 S0 I<id>                         - cancel trigger
 *   M990 S2 I<id>                         - query trigger state
 *
 * Query response:
 *   FLYBY I<id> F<0|1>
 */
@Root
public class SmoothiewareChmtGcodeDriver extends GcodeDriver implements FlyByTriggerDriver {
    private static final int FLYBY_MCODE = 990;

    @Override
    public void armFlyByTrigger(HeadMountable mountable, TriggerRequest request) throws Exception {
        Location location = request.getTriggerLocation().convertToUnits(getUnits());
        String command = String.format(Locale.US,
                "M%d S1 I%d X%.6f Y%.6f P%d",
                FLYBY_MCODE,
                request.getRequestId(),
                location.getX(),
                location.getY(),
                request.getPulseWidthMicroseconds());
        sendCommand(command);
    }

    @Override
    public void cancelFlyByTrigger(long requestId) throws Exception {
        sendCommand(String.format(Locale.US, "M%d S0 I%d", FLYBY_MCODE, requestId));
    }

    @Override
    public boolean hasFlyByTriggerFired(long requestId) throws Exception {
        sendCommand(String.format(Locale.US, "M%d S2 I%d", FLYBY_MCODE, requestId));
        String response = receiveSingleResponse("^FLYBY I" + requestId + " F[01]$");
        Matcher matcher = Pattern.compile("^FLYBY I(\\d+) F([01])$").matcher(response);
        if (!matcher.matches()) {
            throw new Exception("Invalid fly-by trigger response: " + response);
        }
        long responseId = Long.parseLong(matcher.group(1));
        if (responseId != requestId) {
            throw new Exception("Fly-by trigger response id mismatch. Expected " + requestId
                    + " but received " + responseId + ".");
        }
        return "1".equals(matcher.group(2));
    }
}
