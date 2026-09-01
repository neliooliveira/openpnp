/*
 * Copyright (C) 2026 OpenPnP contributors
 *
 * This file is part of OpenPnP.
 *
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 */
package org.openpnp.machine.reference.vision;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.openpnp.spi.Nozzle;
import org.openpnp.spi.TriggeredCamera;
import org.openpnp.spi.TriggeredCamera.TriggeredFrame;
import org.openpnp.spi.TriggeredCamera.TriggerMode;

/**
 * Coordinates deterministic fly-by image acquisition.
 *
 * The manager deliberately does not command motion or process vision. Its responsibility is to
 * keep the relationship between a nozzle, an expected hardware trigger and the returned camera
 * frame explicit. This makes it possible to mix stationary bottom vision and fly-by vision in the
 * same job without relying on implicit frame ordering outside this class.
 */
public class FlyByVisionManager {
    private static final FlyByVisionManager instance = new FlyByVisionManager();

    public static FlyByVisionManager get() {
        return instance;
    }

    private final AtomicLong requestSequence = new AtomicLong();
    private final Map<TriggeredCamera, Deque<CaptureRequest>> pendingByCamera = new HashMap<>();

    private FlyByVisionManager() {
    }

    /**
     * Immutable description of one expected fly-by trigger.
     */
    public static class CaptureRequest {
        private final long requestId;
        private final String nozzleId;
        private final long cameraSequenceBeforeTrigger;

        private CaptureRequest(long requestId, String nozzleId, long cameraSequenceBeforeTrigger) {
            this.requestId = requestId;
            this.nozzleId = nozzleId;
            this.cameraSequenceBeforeTrigger = cameraSequenceBeforeTrigger;
        }

        public long getRequestId() {
            return requestId;
        }

        public String getNozzleId() {
            return nozzleId;
        }

        public long getCameraSequenceBeforeTrigger() {
            return cameraSequenceBeforeTrigger;
        }
    }

    /**
     * The frame associated with a previously armed request.
     */
    public static class CaptureResult {
        private final CaptureRequest request;
        private final TriggeredFrame frame;

        private CaptureResult(CaptureRequest request, TriggeredFrame frame) {
            this.request = request;
            this.frame = frame;
        }

        public CaptureRequest getRequest() {
            return request;
        }

        public TriggeredFrame getFrame() {
            return frame;
        }
    }

    /**
     * Switch a hardware-trigger capable camera into external-trigger mode.
     */
    public synchronized void enterFlyByMode(TriggeredCamera camera) throws Exception {
        if (camera.getTriggerMode() != TriggerMode.External) {
            camera.setTriggerMode(TriggerMode.External);
        }
    }

    /**
     * Return a camera to normal live operation and discard stale pending requests for it.
     */
    public synchronized void enterLiveMode(TriggeredCamera camera) throws Exception {
        pendingByCamera.remove(camera);
        if (camera.getTriggerMode() != TriggerMode.Live) {
            camera.setTriggerMode(TriggerMode.Live);
        }
    }

    /**
     * Arm one expected trigger for a nozzle. Requests are ordered per camera, matching the physical
     * sequence in which a single camera receives trigger pulses.
     */
    public synchronized CaptureRequest arm(TriggeredCamera camera, Nozzle nozzle) throws Exception {
        if (camera == null) {
            throw new IllegalArgumentException("camera must not be null");
        }
        if (nozzle == null) {
            throw new IllegalArgumentException("nozzle must not be null");
        }

        enterFlyByMode(camera);

        CaptureRequest request = new CaptureRequest(
                requestSequence.incrementAndGet(),
                nozzle.getId(),
                camera.getLastTriggerSequence());
        pendingByCamera.computeIfAbsent(camera, key -> new ArrayDeque<>()).addLast(request);
        return request;
    }

    /**
     * Wait for the next hardware-triggered frame and bind it to the oldest armed request for this
     * camera. The camera-provided sequence number is validated so an old/stale frame cannot be
     * silently assigned to a new nozzle request.
     */
    public CaptureResult captureNext(TriggeredCamera camera, long timeoutMilliseconds) throws Exception {
        final CaptureRequest request;
        synchronized (this) {
            Deque<CaptureRequest> queue = pendingByCamera.get(camera);
            if (queue == null || queue.isEmpty()) {
                throw new IllegalStateException("No fly-by capture request is armed for this camera.");
            }
            request = queue.peekFirst();
        }

        TriggeredFrame frame = camera.captureTriggered(timeoutMilliseconds);
        if (frame.getSequence() <= request.getCameraSequenceBeforeTrigger()) {
            throw new IllegalStateException("Stale triggered frame sequence " + frame.getSequence()
                    + " for request " + request.getRequestId() + ", expected sequence after "
                    + request.getCameraSequenceBeforeTrigger() + ".");
        }

        synchronized (this) {
            Deque<CaptureRequest> queue = pendingByCamera.get(camera);
            if (queue == null || queue.peekFirst() != request) {
                throw new IllegalStateException("Fly-by capture request queue changed during acquisition.");
            }
            queue.removeFirst();
            if (queue.isEmpty()) {
                pendingByCamera.remove(camera);
            }
        }
        return new CaptureResult(request, frame);
    }

    /**
     * Cancel all not-yet-captured requests associated with a camera.
     */
    public synchronized void cancel(TriggeredCamera camera) {
        pendingByCamera.remove(camera);
    }

    public synchronized int getPendingCaptureCount(TriggeredCamera camera) {
        Deque<CaptureRequest> queue = pendingByCamera.get(camera);
        return queue == null ? 0 : queue.size();
    }
}
