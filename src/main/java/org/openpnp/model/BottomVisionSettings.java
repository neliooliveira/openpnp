package org.openpnp.model;

import java.awt.geom.Rectangle2D;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.vision.ReferenceBottomVision.MaxRotation;
import org.openpnp.machine.reference.vision.ReferenceBottomVision.PartSettings;
import org.openpnp.machine.reference.vision.ReferenceBottomVision.PartSizeCheckMethod;
import org.openpnp.machine.reference.vision.ReferenceBottomVision.PreRotateUsage;
import org.openpnp.machine.reference.vision.wizards.FlyByBottomVisionSettingsConfigurationWizard;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

public class BottomVisionSettings extends AbstractVisionSettings {

    public enum AcquisitionMode {
        /** Preserve the existing stop-settle-capture bottom vision behavior. */
        Stationary,
        /** Require deterministic hardware-triggered image acquisition while the head is moving. */
        FlyBy,
        /** Use Fly-By when the configured camera and machine support it, otherwise use Stationary. */
        Auto
    }

    @Attribute(required = false)
    protected PreRotateUsage preRotateUsage = PreRotateUsage.Default;

    @Attribute(required = false)
    protected AcquisitionMode acquisitionMode = AcquisitionMode.Stationary;

    @Attribute(required = false)
    protected double flyByApproachDistanceMm = 12.0;

    /** U3V-CAM-IMX296 vendor guidance recommends a trigger pulse of at least 1 ms. */
    @Attribute(required = false)
    protected int flyByCameraPulseMicroseconds = 1000;

    @Attribute(required = false)
    protected int flyByStrobeMicroseconds = 100;

    @Attribute(required = false)
    protected boolean flyByLedStrobe = true;

    @Attribute(required = false)
    protected boolean flyByFallbackToStationary = true;

    @Attribute(required = false)
    protected long flyByCaptureTimeoutMilliseconds = 1000;

    @Attribute(required = false)
    protected PartSizeCheckMethod checkPartSizeMethod = PartSizeCheckMethod.Disabled;

    @Attribute(required = false)
    protected int checkSizeTolerancePercent = 20;

    @Attribute(required = false)
    protected MaxRotation maxRotation = MaxRotation.Adjust;

    @Attribute(required = false)
    protected boolean asymmetric = false;

    @Element(required = false)
    protected Location visionOffset = new Location(LengthUnit.Millimeters);

    @Override
    public Wizard getConfigurationWizard() {
        return new FlyByBottomVisionSettingsConfigurationWizard(this, null);
    }

    public Wizard getConfigurationWizard(PartSettingsHolder settingsHolder) {
        return new FlyByBottomVisionSettingsConfigurationWizard(this, settingsHolder);
    }

    public BottomVisionSettings() {
        super(Configuration.createId("BVS"));
    }

    public BottomVisionSettings(String id) {
        super(id);
    }

    public BottomVisionSettings(PartSettings partSettings) {
        this();
        this.setEnabled(partSettings.isEnabled());
        this.setPipeline(partSettings.getPipeline());
        this.preRotateUsage = partSettings.getPreRotateUsage();
        this.checkPartSizeMethod = partSettings.getCheckPartSizeMethod();
        this.checkSizeTolerancePercent = partSettings.getCheckSizeTolerancePercent();
        this.maxRotation = partSettings.getMaxRotation();
        this.visionOffset = partSettings.getVisionOffset();
        this.asymmetric = this.visionOffset.isInitialized();
    }

    public PreRotateUsage getPreRotateUsage() {
        return preRotateUsage;
    }

    public void setPreRotateUsage(PreRotateUsage preRotateUsage) {
        Object oldValue = this.preRotateUsage;
        this.preRotateUsage = preRotateUsage;
        firePropertyChange("preRotateUsage", oldValue, preRotateUsage);
    }

    public AcquisitionMode getAcquisitionMode() {
        return acquisitionMode;
    }

    public void setAcquisitionMode(AcquisitionMode acquisitionMode) {
        AcquisitionMode oldValue = this.acquisitionMode;
        this.acquisitionMode = acquisitionMode == null ? AcquisitionMode.Stationary : acquisitionMode;
        firePropertyChange("acquisitionMode", oldValue, this.acquisitionMode);
    }

    public double getFlyByApproachDistanceMm() {
        return flyByApproachDistanceMm;
    }

    public void setFlyByApproachDistanceMm(double value) {
        double oldValue = flyByApproachDistanceMm;
        flyByApproachDistanceMm = value;
        firePropertyChange("flyByApproachDistanceMm", oldValue, value);
    }

    public int getFlyByCameraPulseMicroseconds() {
        return flyByCameraPulseMicroseconds;
    }

    public void setFlyByCameraPulseMicroseconds(int value) {
        int oldValue = flyByCameraPulseMicroseconds;
        flyByCameraPulseMicroseconds = value;
        firePropertyChange("flyByCameraPulseMicroseconds", oldValue, value);
    }

    public int getFlyByStrobeMicroseconds() {
        return flyByStrobeMicroseconds;
    }

    public void setFlyByStrobeMicroseconds(int value) {
        int oldValue = flyByStrobeMicroseconds;
        flyByStrobeMicroseconds = value;
        firePropertyChange("flyByStrobeMicroseconds", oldValue, value);
    }

    public boolean isFlyByLedStrobe() {
        return flyByLedStrobe;
    }

    public void setFlyByLedStrobe(boolean value) {
        boolean oldValue = flyByLedStrobe;
        flyByLedStrobe = value;
        firePropertyChange("flyByLedStrobe", oldValue, value);
    }

    public boolean isFlyByFallbackToStationary() {
        return flyByFallbackToStationary;
    }

    public void setFlyByFallbackToStationary(boolean value) {
        boolean oldValue = flyByFallbackToStationary;
        flyByFallbackToStationary = value;
        firePropertyChange("flyByFallbackToStationary", oldValue, value);
    }

    public long getFlyByCaptureTimeoutMilliseconds() {
        return flyByCaptureTimeoutMilliseconds;
    }

    public void setFlyByCaptureTimeoutMilliseconds(long value) {
        long oldValue = flyByCaptureTimeoutMilliseconds;
        flyByCaptureTimeoutMilliseconds = value;
        firePropertyChange("flyByCaptureTimeoutMilliseconds", oldValue, value);
    }

    public PartSizeCheckMethod getCheckPartSizeMethod() {
        return checkPartSizeMethod;
    }

    public void setCheckPartSizeMethod(PartSizeCheckMethod checkPartSizeMethod) {
        Object oldValue = this.checkPartSizeMethod;
        this.checkPartSizeMethod = checkPartSizeMethod;
        firePropertyChange("checkPartSizeMethod", oldValue, checkPartSizeMethod);
    }

    public int getCheckSizeTolerancePercent() {
        return checkSizeTolerancePercent;
    }

    public void setCheckSizeTolerancePercent(int checkSizeTolerancePercent) {
        Object oldValue = this.checkSizeTolerancePercent;
        this.checkSizeTolerancePercent = checkSizeTolerancePercent;
        firePropertyChange("checkSizeTolerancePercent", oldValue, checkSizeTolerancePercent);
    }

    public MaxRotation getMaxRotation() {
        return maxRotation;
    }

    public void setMaxRotation(MaxRotation maxRotation) {
        Object oldValue = this.maxRotation;
        this.maxRotation = maxRotation;
        firePropertyChange("maxRotation", oldValue, maxRotation);
    }

    public boolean isAsymmetric() {
        if (visionOffset.isInitialized()) {
            asymmetric = true;
        }
        return asymmetric;
    }

    public void setAsymmetric(boolean asymmetric) {
        Object oldValue = this.asymmetric;
        this.asymmetric = asymmetric;
        if (!asymmetric) {
            this.setVisionOffset(new Location(LengthUnit.Millimeters));
        }
        firePropertyChange("asymmetric", oldValue, this.asymmetric);
    }

    public Location getVisionOffset() {
        return visionOffset;
    }

    public void setVisionOffset(Location visionOffset) {
        Object oldValue = this.visionOffset;
        this.visionOffset = visionOffset.derive(null, null, 0.0, 0.0);
        firePropertyChange("visionOffset", oldValue, this.visionOffset);
    }

    public void setValues(BottomVisionSettings another) {
        setEnabled(another.isEnabled());
        try {
            setPipeline(another.getPipeline().clone());
        }
        catch (CloneNotSupportedException e) {
        }
        setPipelineParameterAssignments(another.getPipelineParameterAssignments());
        setPreRotateUsage(another.getPreRotateUsage());
        setAcquisitionMode(another.getAcquisitionMode());
        setFlyByApproachDistanceMm(another.getFlyByApproachDistanceMm());
        setFlyByCameraPulseMicroseconds(another.getFlyByCameraPulseMicroseconds());
        setFlyByStrobeMicroseconds(another.getFlyByStrobeMicroseconds());
        setFlyByLedStrobe(another.isFlyByLedStrobe());
        setFlyByFallbackToStationary(another.isFlyByFallbackToStationary());
        setFlyByCaptureTimeoutMilliseconds(another.getFlyByCaptureTimeoutMilliseconds());
        setCheckPartSizeMethod(another.checkPartSizeMethod);
        setMaxRotation(another.getMaxRotation());
        setCheckSizeTolerancePercent(another.getCheckSizeTolerancePercent());
        setVisionOffset(another.getVisionOffset());
        setAsymmetric(another.isAsymmetric());
        Configuration.get().fireVisionSettingsChanged();
    }

    @Override
    public void resetToDefault() {
        BottomVisionSettings stockVisionSettings = (BottomVisionSettings) Configuration.get()
                .getVisionSettings(AbstractVisionSettings.STOCK_BOTTOM_ID);
        setValues(stockVisionSettings);
    }

    public Location getPartCheckSize(Part part, boolean addTolerance) {
        Footprint footprint = part.getPackage().getFootprint();
        double checkWidth = 0.0;
        double checkHeight = 0.0;

        switch (checkPartSizeMethod) {
            case Disabled:
                return null;
            case BodySize:
                checkWidth = footprint.getBodyWidth();
                checkHeight = footprint.getBodyHeight();
                break;
            case PadExtents:
                Rectangle2D bounds = footprint.getPadsShape().getBounds2D();
                checkWidth = bounds.getWidth();
                checkHeight = bounds.getHeight();
                break;
        }
        double factor = addTolerance ? checkSizeTolerancePercent*0.01+1.0 : 1.0;
        return new Location(footprint.getUnits(), checkWidth*factor, checkHeight*factor, 0, 0);
    }

}
