/*
 * Copyright (C) 2026 OpenPnP contributors
 */
package org.openpnp.machine.reference.vision.wizards;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.model.BottomVisionSettings;
import org.openpnp.model.BottomVisionSettings.AcquisitionMode;
import org.openpnp.model.PartSettingsHolder;

/**
 * Compact fly-by acquisition configuration. The existing bottom-vision pipeline remains owned by
 * BottomVisionSettings; this wizard exposes the acquisition parameters that are specific to the
 * deterministic hardware-triggered path.
 */
public class FlyByBottomVisionSettingsConfigurationWizard extends AbstractConfigurationWizard {
    private final BottomVisionSettings settings;
    @SuppressWarnings("unused")
    private final PartSettingsHolder settingsHolder;

    private final JComboBox<AcquisitionMode> acquisitionMode =
            new JComboBox<>(AcquisitionMode.values());
    private final JTextField approachDistance = new JTextField();
    private final JTextField cameraPulse = new JTextField();
    private final JTextField strobePulse = new JTextField();
    private final JTextField captureTimeout = new JTextField();
    private final JCheckBox ledStrobe = new JCheckBox();
    private final JCheckBox fallback = new JCheckBox();

    public FlyByBottomVisionSettingsConfigurationWizard(BottomVisionSettings settings,
            PartSettingsHolder settingsHolder) {
        this.settings = settings;
        this.settingsHolder = settingsHolder;

        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 4));
        panel.add(new JLabel("Acquisition mode"));
        panel.add(acquisitionMode);
        panel.add(new JLabel("Approach distance (mm)"));
        panel.add(approachDistance);
        panel.add(new JLabel("Camera pulse (us)"));
        panel.add(cameraPulse);
        panel.add(new JLabel("LED strobe (us)"));
        panel.add(strobePulse);
        panel.add(new JLabel("Capture timeout (ms)"));
        panel.add(captureTimeout);
        panel.add(new JLabel("Use LED strobe"));
        panel.add(ledStrobe);
        panel.add(new JLabel("Fallback to stationary"));
        panel.add(fallback);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(panel, BorderLayout.NORTH);
        contentPanel.add(wrapper);
    }

    @Override
    public void createBindings() {
        addWrappedBinding(settings, "acquisitionMode", acquisitionMode, "selectedItem");
        addWrappedBinding(settings, "flyByApproachDistanceMm", approachDistance, "text");
        addWrappedBinding(settings, "flyByCameraPulseMicroseconds", cameraPulse, "text");
        addWrappedBinding(settings, "flyByStrobeMicroseconds", strobePulse, "text");
        addWrappedBinding(settings, "flyByCaptureTimeoutMilliseconds", captureTimeout, "text");
        addWrappedBinding(settings, "flyByLedStrobe", ledStrobe, "selected");
        addWrappedBinding(settings, "flyByFallbackToStationary", fallback, "selected");
    }
}
