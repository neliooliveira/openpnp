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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.Action;

import org.openpnp.Translations;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceFeeder;
import org.openpnp.machine.reference.feeder.AprilTagDetector.Detection;
import org.openpnp.machine.reference.feeder.wizards.ReferenceAprilTagFeederConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.util.MovableUtils;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

/**
 * A smart feeder identified and located by an AprilTag. Feeder identity, part assignment and the
 * tag-to-pick offset are persistent; presence and tag location are refreshed by each scan.
 */
public class ReferenceAprilTagFeeder extends ReferenceFeeder {
    @Attribute(required = false)
    private Integer tagId;

    @Element(required = false)
    private Location tagToPickOffset = new Location(LengthUnit.Millimeters);

    private boolean present;

    @Override
    public Location getPickLocation() throws Exception {
        Location pickLocation = tagToPickOffset.offsetWithRotationFrom(location);
        return pickLocation.derive(null, null, null,
                pickLocation.getRotation() + getRotationInTape());
    }

    @Override
    public void feed(Nozzle nozzle) throws Exception {
        verifyReady();

        Machine machine = Configuration.get().getMachine();
        AprilTagFeederProperties properties = new AprilTagFeederProperties(machine);
        Actuator actuator = null;
        if (nozzle != null && nozzle.getHead() != null) {
            actuator = nozzle.getHead().getActuatorByName(properties.getActuatorName());
        }
        if (actuator == null) {
            actuator = machine.getActuatorByName(properties.getActuatorName());
        }
        if (actuator == null) {
            throw new Exception("Feed failed. Unable to find an actuator named "
                    + properties.getActuatorName());
        }

        double pitch = getTapePartPitch().convertToUnits(LengthUnit.Millimeters).getValue();
        if (pitch <= 0) {
            throw new Exception("Feed failed. Tape part pitch must be greater than zero.");
        }
        actuator.actuate(String.format(Locale.US, "%d %.3f", tagId, pitch));
    }

    @Override
    public void prepareForJob(boolean visit) throws Exception {
        verifyReady();
        super.prepareForJob(visit);
    }

    private void verifyReady() throws Exception {
        if (tagId == null) {
            throw new Exception("AprilTag feeder has no tag ID configured.");
        }
        if (!present) {
            throw new Exception("AprilTag feeder " + tagId
                    + " is not present. Scan the feeders before starting the job.");
        }
        if (getPart() == null || getPart().getPackage() == null) {
            throw new Exception("AprilTag feeder " + tagId + " has no part package configured.");
        }
    }

    @Override
    public boolean isEnabled() {
        return super.isEnabled() && present && tagId != null && getPart() != null
                && getPart().getPackage() != null;
    }

    @Override
    public void setPart(Part part) {
        boolean oldEnabled = isEnabled();
        Length oldPitch = getTapePartPitch();
        double oldRotation = getRotationInTape();
        super.setPart(part);
        firePropertyChange("tapePartPitch", oldPitch, getTapePartPitch());
        firePropertyChange("rotationInTape", oldRotation, getRotationInTape());
        firePropertyChange("enabled", oldEnabled, isEnabled());
    }

    public Integer getTagId() {
        return tagId;
    }

    public void setTagId(Integer tagId) {
        if (tagId != null && tagId < 0) {
            throw new IllegalArgumentException("AprilTag ID must not be negative.");
        }
        boolean oldEnabled = isEnabled();
        Integer oldValue = this.tagId;
        this.tagId = tagId;
        firePropertyChange("tagId", oldValue, tagId);
        firePropertyChange("enabled", oldEnabled, isEnabled());
    }

    public Location getTagToPickOffset() {
        return tagToPickOffset;
    }

    public void setTagToPickOffset(Location tagToPickOffset) {
        Location oldValue = this.tagToPickOffset;
        this.tagToPickOffset = tagToPickOffset;
        firePropertyChange("tagToPickOffset", oldValue, tagToPickOffset);
    }

    public boolean isPresent() {
        return present;
    }

    public String getPresenceText() {
        return Translations.getString(present
                ? "ReferenceAprilTagFeederConfigurationWizard.FeederPanel.Present.text"
                : "ReferenceAprilTagFeederConfigurationWizard.FeederPanel.NotPresent.text");
    }

    public void setPresent(boolean present) {
        boolean oldEnabled = isEnabled();
        boolean oldValue = this.present;
        String oldPresenceText = getPresenceText();
        this.present = present;
        firePropertyChange("present", oldValue, present);
        firePropertyChange("presenceText", oldPresenceText, getPresenceText());
        firePropertyChange("enabled", oldEnabled, isEnabled());
    }

    public Length getTapePartPitch() {
        if (getPart() == null || getPart().getPackage() == null) {
            return new Length(4, LengthUnit.Millimeters);
        }
        return getPart().getPackage().getTapePartPitch();
    }

    public double getRotationInTape() {
        if (getPart() == null || getPart().getPackage() == null) {
            return 0;
        }
        return getPart().getPackage().getRotationInTape();
    }

    public static int scan() throws Exception {
        Machine machine = Configuration.get().getMachine();
        Head head = machine.getDefaultHead();
        if (head == null || head.getDefaultCamera() == null) {
            throw new Exception("No default head camera is available for the AprilTag feeder scan.");
        }
        Camera camera = head.getDefaultCamera();
        AprilTagFeederProperties properties = new AprilTagFeederProperties(machine);
        AprilTagDetector detector = new AprilTagDetector(properties.getTagFamily());

        Map<Integer, Detection> closestDetections = new LinkedHashMap<>();
        for (Location scanLocation : createScanLocations(properties.getScanStartLocation(),
                properties.getScanEndLocation(), properties.getScanStepX(),
                properties.getScanStepY())) {
            MovableUtils.moveToLocationAtSafeZ(camera, scanLocation);
            for (Detection detection : detector.detect(camera)) {
                Detection existing = closestDetections.get(detection.getId());
                if (existing == null || detection.getDistanceFromImageCenter()
                        < existing.getDistanceFromImageCenter()) {
                    closestDetections.put(detection.getId(), detection);
                }
            }
        }

        return updateFeeders(machine, closestDetections.values());
    }

    static int updateFeeders(Machine machine, Iterable<Detection> detections) throws Exception {
        for (Feeder feeder : machine.getFeeders()) {
            if (feeder instanceof ReferenceAprilTagFeeder) {
                ((ReferenceAprilTagFeeder) feeder).setPresent(false);
            }
        }

        int count = 0;
        for (Detection detection : detections) {
            ReferenceAprilTagFeeder feeder = findByTagId(machine, detection.getId());
            if (feeder == null) {
                feeder = new ReferenceAprilTagFeeder();
                feeder.setTagId(detection.getId());
                feeder.setName("AprilTag " + detection.getId());
                machine.addFeeder(feeder);
            }
            feeder.setLocation(detection.getLocation());
            feeder.setPresent(true);
            count++;
        }
        return count;
    }

    public static ReferenceAprilTagFeeder findByTagId(int tagId) {
        return findByTagId(Configuration.get().getMachine(), tagId);
    }

    static ReferenceAprilTagFeeder findByTagId(Machine machine, int tagId) {
        for (Feeder feeder : machine.getFeeders()) {
            if (feeder instanceof ReferenceAprilTagFeeder) {
                ReferenceAprilTagFeeder aprilTagFeeder = (ReferenceAprilTagFeeder) feeder;
                if (Integer.valueOf(tagId).equals(aprilTagFeeder.tagId)) {
                    return aprilTagFeeder;
                }
            }
        }
        return null;
    }

    static List<Location> createScanLocations(Location start, Location end, Length stepX,
            Length stepY) {
        Location convertedEnd = end.convertToUnits(start.getUnits());
        double stepXValue = stepX.convertToUnits(start.getUnits()).getValue();
        double stepYValue = stepY.convertToUnits(start.getUnits()).getValue();
        if (stepXValue <= 0 || stepYValue <= 0) {
            throw new IllegalArgumentException("AprilTag scan steps must be greater than zero.");
        }

        int xSegments = segmentCount(Math.abs(convertedEnd.getX() - start.getX()), stepXValue);
        int ySegments = segmentCount(Math.abs(convertedEnd.getY() - start.getY()), stepYValue);
        List<Location> locations = new ArrayList<>();
        for (int yIndex = 0; yIndex <= ySegments; yIndex++) {
            double yRatio = ySegments == 0 ? 0 : yIndex / (double) ySegments;
            double y = interpolate(start.getY(), convertedEnd.getY(), yRatio);
            for (int index = 0; index <= xSegments; index++) {
                int xIndex = yIndex % 2 == 0 ? index : xSegments - index;
                double xRatio = xSegments == 0 ? 0 : xIndex / (double) xSegments;
                double x = interpolate(start.getX(), convertedEnd.getX(), xRatio);
                locations.add(new Location(start.getUnits(), x, y, start.getZ(),
                        start.getRotation()));
            }
        }
        return locations;
    }

    private static int segmentCount(double distance, double maximumStep) {
        return distance == 0 ? 0 : (int) Math.ceil(distance / maximumStep);
    }

    private static double interpolate(double start, double end, double ratio) {
        return start + (end - start) * ratio;
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new ReferenceAprilTagFeederConfigurationWizard(this);
    }

    @Override
    public String getPropertySheetHolderTitle() {
        return getClass().getSimpleName() + " " + getName();
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        return new PropertySheetHolder[0];
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        return new Action[0];
    }
}
