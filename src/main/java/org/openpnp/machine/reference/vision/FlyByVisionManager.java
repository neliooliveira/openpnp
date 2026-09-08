/*
 * Copyright (C) 2026 OpenPnP contributors
 */
package org.openpnp.machine.reference.vision;

import java.util.HashMap;
import java.util.Map;

import org.openpnp.spi.Nozzle;
import org.openpnp.spi.TriggeredCamera;
import org.openpnp.spi.TriggeredCamera.TriggeredFrame;
import org.openpnp.spi.TriggeredCamera.TriggerMode;

/** Associates one deterministic hardware trigger with one nozzle and one returned frame. */
public class FlyByVisionManager {
    private static final long MAX_REQUEST_ID = 65535;
    private static final FlyByVisionManager instance = new FlyByVisionManager();

    public static FlyByVisionManager get() {
        return instance;
    }

    private long requestSequence;
    private final Map<TriggeredCamera, CaptureRequest> pendingByCamera = new HashMap<>();

    private FlyByVisionManager() {
    }

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

    private long nextRequestId() {
        requestSequence++;
        if (requestSequence > MAX_REQUEST_ID) {
            requestSequence = 1;
        }
        return requestSequence;
    }

    public synchronized void enterFlyByMode(TriggeredCamera camera) throws Exception {
        if (camera.getTriggerMode() != TriggerMode.External) {
            camera.setTriggerMode(TriggerMode.External);
        }
    }

    public synchronized void enterLiveMode(TriggeredCamera camera) throws Exception {
        pendingByCamera.remove(camera);
        if (camera.getTriggerMode() != TriggerMode.Live) {
            camera.setTriggerMode(TriggerMode.Live);
        }
    }

    public synchronized CaptureRequest arm(TriggeredCamera camera, Nozzle nozzle) throws Exception {
        if (camera == null || nozzle == null) {
            throw new IllegalArgumentException("camera and nozzle must not be null");
        }
        if (pendingByCamera.containsKey(camera)) {
            throw new IllegalStateException("A fly-by capture is already armed for camera "
                    + camera.getName() + ". Complete or cancel it before arming another nozzle.");
        }
        enterFlyByMode(camera);
        CaptureRequest request = new CaptureRequest(nextRequestId(), nozzle.getId(),
                camera.getLastTriggerSequence());
        pendingByCamera.put(camera, request);
        return request;
    }

    public CaptureResult captureNext(TriggeredCamera camera, long timeoutMilliseconds) throws Exception {
        final CaptureRequest request;
        synchronized (this) {
            request = pendingByCamera.get(camera);
            if (request == null) {
                throw new IllegalStateException("No fly-by capture request is armed for this camera.");
            }
        }
        TriggeredFrame frame = camera.captureTriggered(timeoutMilliseconds);
        if (frame.getSequence() <= request.getCameraSequenceBeforeTrigger()) {
            throw new IllegalStateException("Stale triggered frame sequence " + frame.getSequence()
                    + " for request " + request.getRequestId() + ".");
        }
        synchronized (this) {
            if (pendingByCamera.get(camera) != request) {
                throw new IllegalStateException("Fly-by capture request changed during acquisition.");
            }
            pendingByCamera.remove(camera);
        }
        return new CaptureResult(request, frame);
    }

    public synchronized void cancel(TriggeredCamera camera) {
        pendingByCamera.remove(camera);
    }

    public synchronized int getPendingCaptureCount(TriggeredCamera camera) {
        return pendingByCamera.containsKey(camera) ? 1 : 0;
    }
}
