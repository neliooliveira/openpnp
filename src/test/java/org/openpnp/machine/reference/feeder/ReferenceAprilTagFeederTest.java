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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpnp.machine.reference.feeder.AprilTagDetector.Detection;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Package;
import org.openpnp.model.Part;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Machine;
import org.openpnp.spi.Nozzle;

import com.google.common.io.Files;

import boofcv.alg.drawing.FiducialImageEngine;
import boofcv.alg.fiducial.square.FiducialSquareHammingGenerator;
import boofcv.factory.fiducial.ConfigHammingMarker;
import boofcv.factory.fiducial.HammingDictionary;
import boofcv.struct.image.GrayU8;

public class ReferenceAprilTagFeederTest {
    private ReferenceAprilTagFeeder feeder;
    private Machine machine;

    @BeforeEach
    public void setUp() throws Exception {
        File workingDirectory = new File(Files.createTempDir(), ".openpnp");
        Configuration.initialize(workingDirectory);
        Configuration.get().load();

        machine = Configuration.get().getMachine();
        feeder = new ReferenceAprilTagFeeder();
        machine.addFeeder(feeder);
    }

    @Test
    public void scanGridUsesMaximumStepsAndAlternatingRows() {
        Location start = new Location(LengthUnit.Millimeters, 0, 0, 10, 0);
        Location end = new Location(LengthUnit.Millimeters, 40, 20, 0, 0);

        List<Location> locations = ReferenceAprilTagFeeder.createScanLocations(start, end,
                new Length(25, LengthUnit.Millimeters),
                new Length(25, LengthUnit.Millimeters));

        assertEquals(6, locations.size());
        assertXy(locations.get(0), 0, 0);
        assertXy(locations.get(1), 20, 0);
        assertXy(locations.get(2), 40, 0);
        assertXy(locations.get(3), 40, 20);
        assertXy(locations.get(4), 20, 20);
        assertXy(locations.get(5), 0, 20);
    }

    @Test
    public void pickLocationCombinesTagOffsetAndPackageRotation() throws Exception {
        assignPartWithTapeSettings(2, 45);
        feeder.setLocation(new Location(LengthUnit.Millimeters, 100, 50, 0, 90));
        feeder.setTagToPickOffset(new Location(LengthUnit.Millimeters, 10, 0, 2, 15));

        Location pickLocation = feeder.getPickLocation();

        assertEquals(100, pickLocation.getX(), 0.000001);
        assertEquals(60, pickLocation.getY(), 0.000001);
        assertEquals(2, pickLocation.getZ(), 0.000001);
        assertEquals(150, pickLocation.getRotation(), 0.000001);
    }

    @Test
    public void feederCanOnlyBeEnabledWhenPresentAndConfigured() {
        assignPartWithTapeSettings(4, 0);
        feeder.setTagId(12);
        feeder.setEnabled(true);

        assertFalse(feeder.isEnabled());
        feeder.setPresent(true);
        assertTrue(feeder.isEnabled());
    }

    @Test
    public void feedSendsTagIdAndPackagePitchToActuator() throws Exception {
        assignPartWithTapeSettings(4, 0);
        feeder.setTagId(12);
        feeder.setPresent(true);

        Actuator actuator = mock(Actuator.class);
        when(actuator.getName()).thenReturn(AprilTagFeederProperties.DEFAULT_ACTUATOR_NAME);
        machine.addActuator(actuator);

        feeder.feed(mock(Nozzle.class));

        verify(actuator).actuate("12 4.000");
    }

    @Test
    public void scanUpdatePreservesAbsentFeedersAndCreatesUnknownFeeders() throws Exception {
        assignPartWithTapeSettings(4, 0);
        Part assignedPart = feeder.getPart();
        feeder.setTagId(12);
        feeder.setPresent(true);

        ReferenceAprilTagFeeder absentFeeder = new ReferenceAprilTagFeeder();
        absentFeeder.setTagId(44);
        absentFeeder.setPart(assignedPart);
        absentFeeder.setPresent(true);
        machine.addFeeder(absentFeeder);

        Location updatedLocation = new Location(LengthUnit.Millimeters, 10, 20, 0, 90);
        Location newLocation = new Location(LengthUnit.Millimeters, 30, 40, 0, 0);
        int count = ReferenceAprilTagFeeder.updateFeeders(machine, Arrays.asList(
                new Detection(12, updatedLocation, 0), new Detection(77, newLocation, 0)));

        assertEquals(2, count);
        assertTrue(feeder.isPresent());
        assertEquals(updatedLocation, feeder.getLocation());
        assertFalse(absentFeeder.isPresent());
        assertEquals(assignedPart, absentFeeder.getPart());
        assertTrue(machine.getFeeders().contains(absentFeeder));

        ReferenceAprilTagFeeder newFeeder = ReferenceAprilTagFeeder.findByTagId(machine, 77);
        assertNotNull(newFeeder);
        assertTrue(newFeeder.isPresent());
        assertEquals(newLocation, newFeeder.getLocation());
        assertFalse(newFeeder.isEnabled());
    }

    @Test
    public void detectorCalculatesTagRotationFromCanonicalCorners() {
        Location corner0 = new Location(LengthUnit.Millimeters, 0, 0, 0, 0);
        Location corner1 = new Location(LengthUnit.Millimeters, 0, 10, 0, 0);

        assertEquals(90, AprilTagDetector.calculateRotation(corner0, corner1), 0.000001);
    }

    @Test
    public void detectorFindsRenderedAprilTagWithoutOptionalDeepLearningDependencies()
            throws Exception {
        ConfigHammingMarker config =
                ConfigHammingMarker.loadDictionary(HammingDictionary.APRILTAG_36h11);
        FiducialImageEngine renderer = new FiducialImageEngine();
        renderer.configure(40, 200);
        FiducialSquareHammingGenerator generator = new FiducialSquareHammingGenerator(config);
        generator.setRenderer(renderer);
        generator.setMarkerWidth(200);
        generator.generate(23);

        Camera camera = mock(Camera.class);
        when(camera.lightSettleAndCapture()).thenReturn(toBufferedImage(renderer.getGray()));
        when(camera.getLocation()).thenReturn(new Location(LengthUnit.Millimeters));
        when(camera.getUnitsPerPixelAtZ()).thenReturn(
                new Location(LengthUnit.Millimeters, 0.1, 0.1, 0, 0));

        List<Detection> detections =
                new AprilTagDetector(AprilTagDetector.TagFamily.Tag36h11).detect(camera);

        assertEquals(1, detections.size());
        assertEquals(23, detections.get(0).getId());
    }

    @Test
    public void detectorConvertsBufferedImageToGray() {
        BufferedImage image = new BufferedImage(2, 1, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, 0x000000);
        image.setRGB(1, 0, 0xffffff);

        GrayU8 gray = AprilTagDetector.toGray(image);

        assertEquals(0, gray.get(0, 0));
        assertEquals(255, gray.get(1, 0));
    }

    private void assignPartWithTapeSettings(double pitch, double rotation) {
        Package pkg = new Package("TEST-PACKAGE");
        pkg.setTapePartPitch(new Length(pitch, LengthUnit.Millimeters));
        pkg.setRotationInTape(rotation);
        Part part = new Part("TEST-PART");
        part.setPackage(pkg);
        feeder.setPart(part);
    }

    private static void assertXy(Location location, double x, double y) {
        assertEquals(x, location.getX(), 0.000001);
        assertEquals(y, location.getY(), 0.000001);
    }

    private static BufferedImage toBufferedImage(GrayU8 gray) {
        BufferedImage image = new BufferedImage(gray.width, gray.height,
                BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < gray.height; y++) {
            for (int x = 0; x < gray.width; x++) {
                image.getRaster().setSample(x, y, 0, gray.get(x, y));
            }
        }
        return image;
    }
}
