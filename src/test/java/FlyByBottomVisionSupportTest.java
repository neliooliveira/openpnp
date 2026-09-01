import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.openpnp.machine.reference.driver.VirtualFlyByDriver;
import org.openpnp.machine.reference.vision.FlyByBottomVisionSupport;
import org.openpnp.machine.reference.vision.FlyByVisionManager.CaptureResult;
import org.openpnp.model.BottomVisionSettings;
import org.openpnp.model.BottomVisionSettings.AcquisitionMode;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;
import org.openpnp.spi.TriggeredCamera;
import org.openpnp.spi.TriggeredCamera.TriggerMode;

public class FlyByBottomVisionSupportTest {
    @Test
    public void testAcquisitionModeSelection() {
        BottomVisionSettings settings = new BottomVisionSettings();
        TriggeredCamera triggeredCamera = createTriggeredCamera(new AtomicInteger());
        Camera stationaryCamera = createStationaryCamera(new AtomicInteger());
        VirtualFlyByDriver driver = new VirtualFlyByDriver();

        settings.setAcquisitionMode(AcquisitionMode.Stationary);
        assertFalse(FlyByBottomVisionSupport.shouldUseFlyBy(settings, triggeredCamera, driver));

        settings.setAcquisitionMode(AcquisitionMode.FlyBy);
        assertTrue(FlyByBottomVisionSupport.shouldUseFlyBy(settings, triggeredCamera, driver));
        assertFalse(FlyByBottomVisionSupport.shouldUseFlyBy(settings, stationaryCamera, driver));

        settings.setAcquisitionMode(AcquisitionMode.Auto);
        assertTrue(FlyByBottomVisionSupport.shouldUseFlyBy(settings, triggeredCamera, driver));
        assertFalse(FlyByBottomVisionSupport.shouldUseFlyBy(settings, stationaryCamera, driver));
    }

    @Test
    public void testCapabilityValidationAndFallback() throws Exception {
        BottomVisionSettings settings = new BottomVisionSettings();
        Camera stationaryCamera = createStationaryCamera(new AtomicInteger());

        settings.setAcquisitionMode(AcquisitionMode.FlyBy);
        settings.setFlyByFallbackToStationary(false);
        assertThrows(Exception.class,
                () -> FlyByBottomVisionSupport.validate(settings, stationaryCamera, null));

        settings.setFlyByFallbackToStationary(true);
        assertDoesNotThrow(() -> FlyByBottomVisionSupport.validate(settings, stationaryCamera, null));

        settings.setAcquisitionMode(AcquisitionMode.Auto);
        settings.setFlyByApproachDistanceMm(Double.NaN);
        assertDoesNotThrow(() -> FlyByBottomVisionSupport.validate(settings, stationaryCamera, null));
    }

    @Test
    public void testApproachLocationUsesTravelDirection() {
        Location current = new Location(LengthUnit.Millimeters, 0, 0, 2, 30);
        Location shot = new Location(LengthUnit.Millimeters, 6, 8, 3, 45);

        Location approach = FlyByBottomVisionSupport.getApproachLocation(current, shot, 5);

        assertTrue(Math.abs(approach.getX() - 3) < 0.000001);
        assertTrue(Math.abs(approach.getY() - 4) < 0.000001);
        assertTrue(Math.abs(approach.getZ() - 3) < 0.000001);
        assertTrue(Math.abs(approach.getRotation() - 45) < 0.000001);
    }

    @Test
    public void testTriggeredFrameIsUsedWithoutSourceCameraCapture() throws Exception {
        AtomicInteger sourceCaptures = new AtomicInteger();
        Camera sourceCamera = createStationaryCamera(sourceCaptures);
        BufferedImage frame = new BufferedImage(16, 12, BufferedImage.TYPE_BYTE_GRAY);
        CaptureResult result = createCaptureResult(frame);

        Camera pipelineCamera = FlyByBottomVisionSupport.createPipelineCamera(sourceCamera, result);

        assertSame(frame, pipelineCamera.capture());
        assertTrue(sourceCaptures.get() == 0);
    }

    private static CaptureResult createCaptureResult(BufferedImage image) throws Exception {
        java.lang.reflect.Constructor<CaptureResult> constructor = CaptureResult.class
                .getDeclaredConstructor(
                        org.openpnp.machine.reference.vision.FlyByVisionManager.CaptureRequest.class,
                        TriggeredCamera.TriggeredFrame.class);
        constructor.setAccessible(true);
        return constructor.newInstance(null,
                new TriggeredCamera.TriggeredFrame(image, 1, System.nanoTime()));
    }

    private static Camera createStationaryCamera(AtomicInteger captures) {
        return (Camera) Proxy.newProxyInstance(Camera.class.getClassLoader(),
                new Class<?>[] { Camera.class }, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "capture":
                            captures.incrementAndGet();
                            return new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_GRAY);
                        case "getUnitsPerPixelAtZ":
                            return new Location(LengthUnit.Millimeters, 0.01, 0.01, 0, 0);
                        case "hashCode":
                            return System.identityHashCode(proxy);
                        case "equals":
                            return proxy == args[0];
                        default:
                            return null;
                    }
                });
    }

    private static TriggeredCamera createTriggeredCamera(AtomicInteger captures) {
        return (TriggeredCamera) Proxy.newProxyInstance(TriggeredCamera.class.getClassLoader(),
                new Class<?>[] { TriggeredCamera.class }, (proxy, method, args) -> {
                    if ("getTriggerMode".equals(method.getName())) {
                        return TriggerMode.Live;
                    }
                    if ("capture".equals(method.getName())) {
                        captures.incrementAndGet();
                    }
                    if ("hashCode".equals(method.getName())) {
                        return System.identityHashCode(proxy);
                    }
                    if ("equals".equals(method.getName())) {
                        return proxy == args[0];
                    }
                    return null;
                });
    }
}
