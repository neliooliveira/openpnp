/*
 * Copyright (C) 2026 OpenPnP contributors
 */
package org.openpnp.machine.reference.camera.genicam;

import java.awt.image.BufferedImage;

/**
 * Vendor-neutral backend contract for GenICam / USB3 Vision cameras.
 *
 * Concrete backends may use a GenTL producer, Aravis, a vendor SDK or another transport layer, but
 * the OpenPnP camera code only depends on standard GenICam concepts and this interface.
 */
public interface GenicamBackend extends AutoCloseable {
    public static class Frame {
        private final BufferedImage image;
        private final long sequence;
        private final long timestampNanos;

        public Frame(BufferedImage image, long sequence, long timestampNanos) {
            if (image == null) {
                throw new IllegalArgumentException("image must not be null");
            }
            this.image = image;
            this.sequence = sequence;
            this.timestampNanos = timestampNanos;
        }

        public BufferedImage getImage() {
            return image;
        }

        public long getSequence() {
            return sequence;
        }

        public long getTimestampNanos() {
            return timestampNanos;
        }
    }

    void open(String deviceSelector) throws Exception;

    boolean isOpen();

    void startAcquisition() throws Exception;

    void stopAcquisition() throws Exception;

    Frame grab(long timeoutMilliseconds) throws Exception;

    /** Set a GenICam enum node, e.g. TriggerMode=On or TriggerSource=Line1. */
    void setEnum(String nodeName, String value) throws Exception;

    /** Set a GenICam floating-point node, e.g. ExposureTime. */
    void setFloat(String nodeName, double value) throws Exception;

    /** Set a GenICam integer node, e.g. Width, Height, OffsetX, OffsetY. */
    void setInteger(String nodeName, long value) throws Exception;

    String getEnum(String nodeName) throws Exception;

    double getFloat(String nodeName) throws Exception;

    long getInteger(String nodeName) throws Exception;

    @Override
    void close() throws Exception;
}
