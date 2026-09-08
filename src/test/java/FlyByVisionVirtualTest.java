import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.lang.reflect.Proxy;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.openpnp.machine.reference.vision.FlyByVisionExecutor;
import org.openpnp.machine.reference.vision.FlyByVisionManager;
import org.openpnp.machine.reference.vision.FlyByVisionManager.CaptureRequest;
import org.openpnp.machine.reference.vision.FlyByVisionManager.CaptureResult;
import org.openpnp.machine.reference.vision.FlyByVisionProcessingExecutor;
import org.openpnp.spi.FlyByTriggerDriver;
import org.openpnp.spi.FlyByTriggerDriver.FlyByMode;
import org.openpnp.spi.FlyByTriggerDriver.TriggerRequest;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.TriggeredCamera;
import org.openpnp.spi.TriggeredCamera.TriggerMode;
import org.openpnp.spi.TriggeredCamera.TriggeredFrame;

public class FlyByVisionVirtualTest {
    @Test
    public void testSuccessfulVirtualFlyByCycle() throws Exception {
        AtomicReference<TriggerMode> cameraMode = new AtomicReference<>(TriggerMode.Live);
        AtomicLong cameraSequence = new AtomicLong(10);
        BufferedImage expectedImage = new BufferedImage(32, 32, BufferedImage.TYPE_BYTE_GRAY);
        TriggeredCamera camera = createCamera(cameraMode, cameraSequence, 11, expectedImage);
        Nozzle nozzle = createNozzle("N1");

        AtomicReference<FlyByMode> driverMode = new AtomicReference<>(FlyByMode.Live);
        AtomicReference<TriggerRequest> armedRequest = new AtomicReference<>();
        FlyByTriggerDriver driver = createDriver(driverMode, armedRequest);

        FlyByVisionExecutor executor = new FlyByVisionExecutor();
        CaptureRequest request = executor.arm(camera, driver, nozzle, 1, 12.5, true);

        assertEquals(TriggerMode.External, cameraMode.get());
        assertEquals(FlyByMode.Trigger, driverMode.get());
        assertEquals(request.getRequestId(), armedRequest.get().getRequestId());
        assertEquals(1, armedRequest.get().getNozzleId());
        assertEquals(12.5, armedRequest.get().getTriggerDistanceMillimeters(), 0.000001);
        assertTrue(armedRequest.get().isCameraTrigger());
        assertTrue(armedRequest.get().isLedStrobe());

        CaptureResult result = executor.complete(camera, driver, request, 1000);

        assertEquals(request.getRequestId(), result.getRequest().getRequestId());
        assertEquals(11, result.getFrame().getSequence());
        assertSame(expectedImage, result.getFrame().getImage());
        assertEquals(TriggerMode.Live, cameraMode.get());
        assertEquals(FlyByMode.Live, driverMode.get());
        assertEquals(0, FlyByVisionManager.get().getPendingCaptureCount(camera));
    }

    @Test
    public void testStaleFrameIsRejectedAndStateReturnsLive() throws Exception {
        AtomicReference<TriggerMode> cameraMode = new AtomicReference<>(TriggerMode.Live);
        AtomicLong cameraSequence = new AtomicLong(20);
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_BYTE_GRAY);
        TriggeredCamera camera = createCamera(cameraMode, cameraSequence, 20, image);
        Nozzle nozzle = createNozzle("N1");

        AtomicReference<FlyByMode> driverMode = new AtomicReference<>(FlyByMode.Live);
        FlyByTriggerDriver driver = createDriver(driverMode, new AtomicReference<TriggerRequest>());
        FlyByVisionExecutor executor = new FlyByVisionExecutor();
        CaptureRequest request = executor.arm(camera, driver, nozzle, 1, 5.0, false);

        assertThrows(IllegalStateException.class,
                () -> executor.complete(camera, driver, request, 1000));

        assertEquals(TriggerMode.Live, cameraMode.get());
        assertEquals(FlyByMode.Live, driverMode.get());
        assertEquals(0, FlyByVisionManager.get().getPendingCaptureCount(camera));
    }

    @Test
    public void testOnlyOneOutstandingRequestPerCamera() throws Exception {
        AtomicReference<TriggerMode> cameraMode = new AtomicReference<>(TriggerMode.Live);
        AtomicLong cameraSequence = new AtomicLong(30);
        BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_BYTE_GRAY);
        TriggeredCamera camera = createCamera(cameraMode, cameraSequence, 31, image);
        Nozzle nozzle1 = createNozzle("N1");
        Nozzle nozzle2 = createNozzle("N2");
        FlyByVisionManager manager = FlyByVisionManager.get();

        try {
            manager.arm(camera, nozzle1);
            assertThrows(IllegalStateException.class, () -> manager.arm(camera, nozzle2));
        }
        finally {
            manager.enterLiveMode(camera);
        }

        assertEquals(0, manager.getPendingCaptureCount(camera));
        assertEquals(TriggerMode.Live, cameraMode.get());
    }

    @Test
    public void testTriggerRequestProtocolBounds() {
        assertThrows(IllegalArgumentException.class,
                () -> new TriggerRequest(0, 1, 1.0, true, true));
        assertThrows(IllegalArgumentException.class,
                () -> new TriggerRequest(65536, 1, 1.0, true, true));
        assertThrows(IllegalArgumentException.class,
                () -> new TriggerRequest(1, 0, 1.0, true, true));
        assertThrows(IllegalArgumentException.class,
                () -> new TriggerRequest(1, 256, 1.0, true, true));
        assertThrows(IllegalArgumentException.class,
                () -> new TriggerRequest(1, 1, 0.0, true, true));
        assertThrows(IllegalArgumentException.class,
                () -> new TriggerRequest(1, 1, Double.NaN, true, true));
    }

    @Test
    public void testFrameProcessingCanRunInParallel() throws Exception {
        FlyByVisionProcessingExecutor processing = new FlyByVisionProcessingExecutor(2);
        CountDownLatch bothStarted = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger concurrent = new AtomicInteger();
        AtomicInteger maximumConcurrent = new AtomicInteger();

        try {
            Future<Integer> first = processing.submit(() -> runVirtualProcessingTask(
                    bothStarted, release, concurrent, maximumConcurrent));
            Future<Integer> second = processing.submit(() -> runVirtualProcessingTask(
                    bothStarted, release, concurrent, maximumConcurrent));

            assertTrue(bothStarted.await(2, TimeUnit.SECONDS));
            release.countDown();
            assertEquals(1, first.get(2, TimeUnit.SECONDS));
            assertEquals(1, second.get(2, TimeUnit.SECONDS));
            assertEquals(2, maximumConcurrent.get());
        }
        finally {
            processing.close();
        }
    }

    private static int runVirtualProcessingTask(CountDownLatch bothStarted, CountDownLatch release,
            AtomicInteger concurrent, AtomicInteger maximumConcurrent) throws Exception {
        int active = concurrent.incrementAndGet();
        maximumConcurrent.accumulateAndGet(active, Math::max);
        bothStarted.countDown();
        try {
            if (!release.await(2, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Virtual processing synchronization timed out.");
            }
            return 1;
        }
        finally {
            concurrent.decrementAndGet();
        }
    }

    private static Nozzle createNozzle(String id) {
        return (Nozzle) Proxy.newProxyInstance(Nozzle.class.getClassLoader(),
                new Class<?>[] { Nozzle.class }, (proxy, method, args) -> {
                    if ("getId".equals(method.getName())) {
                        return id;
                    }
                    return handleObjectMethod(proxy, method.getName(), args);
                });
    }

    private static TriggeredCamera createCamera(AtomicReference<TriggerMode> mode,
            AtomicLong lastSequence, long capturedSequence, BufferedImage image) {
        return (TriggeredCamera) Proxy.newProxyInstance(TriggeredCamera.class.getClassLoader(),
                new Class<?>[] { TriggeredCamera.class }, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "setTriggerMode":
                            mode.set((TriggerMode) args[0]);
                            return null;
                        case "getTriggerMode":
                            return mode.get();
                        case "getLastTriggerSequence":
                            return lastSequence.get();
                        case "captureTriggered":
                            lastSequence.set(capturedSequence);
                            return new TriggeredFrame(image, capturedSequence, 123456789L);
                        case "getName":
                            return "Virtual Fly-By Camera";
                        default:
                            return handleObjectMethod(proxy, method.getName(), args);
                    }
                });
    }

    private static FlyByTriggerDriver createDriver(AtomicReference<FlyByMode> mode,
            AtomicReference<TriggerRequest> armedRequest) {
        return (FlyByTriggerDriver) Proxy.newProxyInstance(FlyByTriggerDriver.class.getClassLoader(),
                new Class<?>[] { FlyByTriggerDriver.class }, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "setFlyByMode":
                            mode.set((FlyByMode) args[0]);
                            return null;
                        case "armFlyByTrigger":
                            armedRequest.set((TriggerRequest) args[1]);
                            return null;
                        case "hasFlyByTriggerFired":
                            return true;
                        case "setFlyByTiming":
                        case "cancelFlyByTrigger":
                            return null;
                        default:
                            return handleObjectMethod(proxy, method.getName(), args);
                    }
                });
    }

    private static Object handleObjectMethod(Object proxy, String methodName, Object[] args) {
        switch (methodName) {
            case "hashCode":
                return System.identityHashCode(proxy);
            case "equals":
                return proxy == args[0];
            case "toString":
                return proxy.getClass().getInterfaces()[0].getSimpleName() + " virtual proxy";
            default:
                return null;
        }
    }
}
