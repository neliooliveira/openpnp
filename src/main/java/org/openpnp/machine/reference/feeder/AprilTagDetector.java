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

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.openpnp.model.Location;
import org.openpnp.spi.Camera;
import org.openpnp.util.VisionUtils;

import boofcv.abst.fiducial.FiducialDetector;
import boofcv.factory.fiducial.ConfigHammingMarker;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.factory.fiducial.HammingDictionary;
import boofcv.struct.image.GrayU8;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;

/** Locates AprilTags in a camera image and converts their centers and rotations to machine space. */
public class AprilTagDetector {
    public enum TagFamily {
        Tag16h5(HammingDictionary.APRILTAG_16h5),
        Tag25h9(HammingDictionary.APRILTAG_25h9),
        Tag36h10(HammingDictionary.APRILTAG_36h10),
        Tag36h11(HammingDictionary.APRILTAG_36h11);

        private final HammingDictionary dictionary;

        TagFamily(HammingDictionary dictionary) {
            this.dictionary = dictionary;
        }

        HammingDictionary getDictionary() {
            return dictionary;
        }
    }

    public static class Detection {
        private final int id;
        private final Location location;
        private final double distanceFromImageCenter;

        Detection(int id, Location location, double distanceFromImageCenter) {
            this.id = id;
            this.location = location;
            this.distanceFromImageCenter = distanceFromImageCenter;
        }

        public int getId() {
            return id;
        }

        public Location getLocation() {
            return location;
        }

        public double getDistanceFromImageCenter() {
            return distanceFromImageCenter;
        }
    }

    private final FiducialDetector<GrayU8> detector;

    public AprilTagDetector(TagFamily tagFamily) {
        ConfigHammingMarker markerConfig =
                ConfigHammingMarker.loadDictionary(tagFamily.getDictionary());
        detector = FactoryFiducial.squareHamming(markerConfig, null, GrayU8.class);
    }

    public List<Detection> detect(Camera camera) throws Exception {
        BufferedImage image = camera.lightSettleAndCapture();
        detector.detect(toGray(image));

        List<Detection> detections = new ArrayList<>();
        Point2D_F64 center = new Point2D_F64();
        for (int i = 0; i < detector.totalFound(); i++) {
            detector.getCenter(i, center);
            Polygon2D_F64 bounds = detector.getBounds(i, null);
            Point2D_F64 corner0Pixel = bounds.get(0);
            Point2D_F64 corner1Pixel = bounds.get(1);
            Location corner0 = VisionUtils.getPixelLocation(camera, corner0Pixel.x,
                    corner0Pixel.y);
            Location corner1 = VisionUtils.getPixelLocation(camera, corner1Pixel.x,
                    corner1Pixel.y);
            double rotation = calculateRotation(corner0, corner1);
            Location location = VisionUtils.getPixelLocation(camera, center.x, center.y)
                    .derive(null, null, null, rotation);
            double distanceFromCenter = Math.hypot(center.x - image.getWidth() / 2.0,
                    center.y - image.getHeight() / 2.0);
            detections.add(new Detection(Math.toIntExact(detector.getId(i)), location,
                    distanceFromCenter));
        }
        return detections;
    }

    static GrayU8 toGray(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = image.getRGB(0, 0, width, height, null, 0, width);
        GrayU8 gray = new GrayU8(width, height);
        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            int red = (pixel >> 16) & 0xff;
            int green = (pixel >> 8) & 0xff;
            int blue = pixel & 0xff;
            gray.data[i] = (byte) ((red * 77 + green * 150 + blue * 29) >> 8);
        }
        return gray;
    }

    static double calculateRotation(Location corner0, Location corner1) {
        Location convertedCorner1 = corner1.convertToUnits(corner0.getUnits());
        return Math.toDegrees(Math.atan2(convertedCorner1.getY() - corner0.getY(),
                convertedCorner1.getX() - corner0.getX()));
    }
}
