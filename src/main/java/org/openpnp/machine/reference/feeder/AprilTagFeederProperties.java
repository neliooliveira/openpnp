/*
 * Copyright (C) 2026 Nélio Oliveira
 *
 * This file is part of OpenPnP.
 *
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * OpenPnP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with OpenPnP. If not, see
 * <http://www.gnu.org/licenses/>.
 *
 * For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.machine.reference.feeder;

import org.openpnp.machine.reference.feeder.AprilTagDetector.TagFamily;
import org.openpnp.model.AbstractModelObject;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Machine;

/** Machine-wide settings shared by all {@link ReferenceAprilTagFeeder} instances. */
public class AprilTagFeederProperties extends AbstractModelObject {
    static final String ACTUATOR_NAME = "ReferenceAprilTagFeeder.ActuatorName";
    static final String SCAN_END_LOCATION = "ReferenceAprilTagFeeder.ScanEndLocation";
    static final String SCAN_START_LOCATION = "ReferenceAprilTagFeeder.ScanStartLocation";
    static final String SCAN_STEP_X = "ReferenceAprilTagFeeder.ScanStepX";
    static final String SCAN_STEP_Y = "ReferenceAprilTagFeeder.ScanStepY";
    static final String TAG_FAMILY = "ReferenceAprilTagFeeder.TagFamily";

    public static final String DEFAULT_ACTUATOR_NAME = "APRILTAGFEEDER";

    private final Machine machine;

    public AprilTagFeederProperties(Machine machine) {
        this.machine = machine;
    }

    public String getActuatorName() {
        String value = (String) machine.getProperty(ACTUATOR_NAME);
        return value == null ? DEFAULT_ACTUATOR_NAME : value;
    }

    public void setActuatorName(String actuatorName) {
        String oldValue = getActuatorName();
        machine.setProperty(ACTUATOR_NAME, actuatorName);
        firePropertyChange("actuatorName", oldValue, actuatorName);
    }

    public Location getScanStartLocation() {
        Location value = (Location) machine.getProperty(SCAN_START_LOCATION);
        return value == null ? new Location(LengthUnit.Millimeters) : value;
    }

    public void setScanStartLocation(Location scanStartLocation) {
        Location oldValue = getScanStartLocation();
        machine.setProperty(SCAN_START_LOCATION, scanStartLocation);
        firePropertyChange("scanStartLocation", oldValue, scanStartLocation);
    }

    public Location getScanEndLocation() {
        Location value = (Location) machine.getProperty(SCAN_END_LOCATION);
        return value == null ? new Location(LengthUnit.Millimeters) : value;
    }

    public void setScanEndLocation(Location scanEndLocation) {
        Location oldValue = getScanEndLocation();
        machine.setProperty(SCAN_END_LOCATION, scanEndLocation);
        firePropertyChange("scanEndLocation", oldValue, scanEndLocation);
    }

    public Length getScanStepX() {
        Length value = (Length) machine.getProperty(SCAN_STEP_X);
        return value == null ? new Length(25, LengthUnit.Millimeters) : value;
    }

    public void setScanStepX(Length scanStepX) {
        Length oldValue = getScanStepX();
        machine.setProperty(SCAN_STEP_X, scanStepX);
        firePropertyChange("scanStepX", oldValue, scanStepX);
    }

    public Length getScanStepY() {
        Length value = (Length) machine.getProperty(SCAN_STEP_Y);
        return value == null ? new Length(25, LengthUnit.Millimeters) : value;
    }

    public void setScanStepY(Length scanStepY) {
        Length oldValue = getScanStepY();
        machine.setProperty(SCAN_STEP_Y, scanStepY);
        firePropertyChange("scanStepY", oldValue, scanStepY);
    }

    public TagFamily getTagFamily() {
        String value = (String) machine.getProperty(TAG_FAMILY);
        if (value != null) {
            try {
                return TagFamily.valueOf(value);
            }
            catch (IllegalArgumentException ignored) {
            }
        }
        return TagFamily.Tag36h11;
    }

    public void setTagFamily(TagFamily tagFamily) {
        TagFamily oldValue = getTagFamily();
        machine.setProperty(TAG_FAMILY, tagFamily.name());
        firePropertyChange("tagFamily", oldValue, tagFamily);
    }
}
