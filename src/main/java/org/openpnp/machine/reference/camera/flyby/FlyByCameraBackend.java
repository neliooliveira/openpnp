/*
 * Copyright (C) 2026 OpenPnP contributors
 */
package org.openpnp.machine.reference.camera.flyby;

import java.awt.image.BufferedImage;

/**
 * Vendor-neutral backend contract for Fly-By cameras.
 *
 * The initial implementation targets GenICam / USB3 Vision hardware, but this abstraction is named
 * after the OpenPnP capability rather than a transport standard so other deterministic triggered
 * cameras can be supported without changing Fly-By vision.
 */
public interface FlyByCameraBackend extends AutoCloseable {
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

        public BufferedImage getImage() { return image; }
        public long getSequence() { return sequence; }
        public long getTimestampNanos() { return timestampNanos; }
    }

    void open(String deviceSelector) throws Exception;
    boolean isOpen();
    void startAcquisition() throws Exception;
    void stopAcquisition() throws Exception;
    Frame grab(long timeoutMilliseconds) throws Exception;

    void setEnum(String nodeName, String value) throws Exception;
    void setFloat(String nodeName, double value) throws Exception;
    void setInteger(String nodeName, long value) throws Exception;
    String getEnum(String nodeName) throws Exception;
    double getFloat(String nodeName) throws Exception;
    long getInteger(String nodeName) throws Exception;

    @Override
    void close() throws Exception;
}
