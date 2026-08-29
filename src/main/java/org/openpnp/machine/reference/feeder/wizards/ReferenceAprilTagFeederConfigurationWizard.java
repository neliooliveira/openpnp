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

package org.openpnp.machine.reference.feeder.wizards;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.Converter;
import org.openpnp.Translations;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.LocationButtonsPanel;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.machine.reference.feeder.AprilTagDetector.TagFamily;
import org.openpnp.machine.reference.feeder.AprilTagFeederProperties;
import org.openpnp.machine.reference.feeder.ReferenceAprilTagFeeder;
import org.openpnp.model.Configuration;
import org.openpnp.util.UiUtils;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class ReferenceAprilTagFeederConfigurationWizard
        extends AbstractReferenceFeederConfigurationWizard {
    private final ReferenceAprilTagFeeder feeder;
    private final AprilTagFeederProperties properties;

    private JTextField tagId;
    private JLabel presence;
    private JTextField partPitch;
    private JTextField rotationInTape;

    private JTextField tagX;
    private JTextField tagY;
    private JTextField tagZ;
    private JTextField tagRotation;

    private JTextField offsetX;
    private JTextField offsetY;
    private JTextField offsetZ;
    private JTextField offsetRotation;
    private LocationButtonsPanel offsetLocationButtons;

    private JTextField scanStartX;
    private JTextField scanStartY;
    private JTextField scanEndX;
    private JTextField scanEndY;
    private JTextField scanStepX;
    private JTextField scanStepY;
    private JComboBox<TagFamily> tagFamily;
    private JTextField actuatorName;
    private JButton scanButton;
    private JLabel scanStatus;

    public ReferenceAprilTagFeederConfigurationWizard(ReferenceAprilTagFeeder feeder) {
        super(feeder, false);
        this.feeder = feeder;
        properties = new AprilTagFeederProperties(Configuration.get().getMachine());
        createUi();
    }

    private void createUi() {
        createFeederPanel();
        createTagLocationPanel();
        createOffsetPanel();
        createScanPanel();
    }

    private void createFeederPanel() {
        JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder(null, Translations.getString(
                "ReferenceAprilTagFeederConfigurationWizard.FeederPanel.Border.title"), //$NON-NLS-1$
                TitledBorder.LEADING, TitledBorder.TOP, null));
        contentPanel.add(panel);
        panel.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));

        panel.add(new JLabel(Translations.getString(
                "ReferenceAprilTagFeederConfigurationWizard.FeederPanel.TagIdLabel.text")), //$NON-NLS-1$
                "2, 2, right, default");
        tagId = new JTextField(10);
        panel.add(tagId, "4, 2, left, default");

        panel.add(new JLabel(Translations.getString(
                "ReferenceAprilTagFeederConfigurationWizard.FeederPanel.PresenceLabel.text")), //$NON-NLS-1$
                "2, 4, right, default");
        presence = new JLabel();
        panel.add(presence, "4, 4, left, default");

        panel.add(new JLabel(Translations.getString(
                "ReferenceAprilTagFeederConfigurationWizard.FeederPanel.PartPitchLabel.text")), //$NON-NLS-1$
                "2, 6, right, default");
        partPitch = new JTextField(10);
        partPitch.setEditable(false);
        panel.add(partPitch, "4, 6, left, default");

        panel.add(new JLabel(Translations.getString(
                "ReferenceAprilTagFeederConfigurationWizard.FeederPanel.RotationInTapeLabel.text")), //$NON-NLS-1$
                "2, 8, right, default");
        rotationInTape = new JTextField(10);
        rotationInTape.setEditable(false);
        panel.add(rotationInTape, "4, 8, left, default");
    }

    private void createTagLocationPanel() {
        JPanel panel = createLocationPanel(Translations.getString(
                "ReferenceAprilTagFeederConfigurationWizard.TagLocationPanel.Border.title")); //$NON-NLS-1$
        tagX = new JTextField(8);
        tagY = new JTextField(8);
        tagZ = new JTextField(8);
        tagRotation = new JTextField(8);
        tagX.setEditable(false);
        tagY.setEditable(false);
        tagZ.setEditable(false);
        tagRotation.setEditable(false);
        addLocationFields(panel, tagX, tagY, tagZ, tagRotation);
    }

    private void createOffsetPanel() {
        JPanel panel = createLocationPanel(Translations.getString(
                "ReferenceAprilTagFeederConfigurationWizard.OffsetPanel.Border.title")); //$NON-NLS-1$
        offsetX = new JTextField(8);
        offsetY = new JTextField(8);
        offsetZ = new JTextField(8);
        offsetRotation = new JTextField(8);
        addLocationFields(panel, offsetX, offsetY, offsetZ, offsetRotation);
        offsetLocationButtons = new LocationButtonsPanel(offsetX, offsetY, offsetZ, offsetRotation);
        panel.add(offsetLocationButtons, "10, 4");
    }

    private JPanel createLocationPanel(String title) {
        JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder(null, title, TitledBorder.LEADING, TitledBorder.TOP, null));
        contentPanel.add(panel);
        panel.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));
        panel.add(new JLabel("X"), "2, 2, center, default");
        panel.add(new JLabel("Y"), "4, 2, center, default");
        panel.add(new JLabel("Z"), "6, 2, center, default");
        panel.add(new JLabel(Translations.getString(
                "ReferenceAprilTagFeederConfigurationWizard.RotationLabel.text")), //$NON-NLS-1$
                "8, 2, center, default");
        return panel;
    }

    private void addLocationFields(JPanel panel, JTextField x, JTextField y, JTextField z,
            JTextField rotation) {
        panel.add(x, "2, 4");
        panel.add(y, "4, 4");
        panel.add(z, "6, 4");
        panel.add(rotation, "8, 4");
    }

    private void createScanPanel() {
        JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder(null, Translations.getString(
                "ReferenceAprilTagFeederConfigurationWizard.ScanPanel.Border.title"), //$NON-NLS-1$
                TitledBorder.LEADING, TitledBorder.TOP, null));
        contentPanel.add(panel);
        panel.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));

        panel.add(new JLabel("X"), "4, 2, center, default");
        panel.add(new JLabel("Y"), "6, 2, center, default");

        panel.add(new JLabel(Translations.getString(
                "ReferenceAprilTagFeederConfigurationWizard.ScanPanel.StartLabel.text")), //$NON-NLS-1$
                "2, 4, right, default");
        scanStartX = new JTextField(8);
        scanStartY = new JTextField(8);
        panel.add(scanStartX, "4, 4");
        panel.add(scanStartY, "6, 4");
        panel.add(new LocationButtonsPanel(scanStartX, scanStartY, (JTextField) null,
                (JTextField) null), "8, 4");

        panel.add(new JLabel(Translations.getString(
                "ReferenceAprilTagFeederConfigurationWizard.ScanPanel.EndLabel.text")), //$NON-NLS-1$
                "2, 6, right, default");
        scanEndX = new JTextField(8);
        scanEndY = new JTextField(8);
        panel.add(scanEndX, "4, 6");
        panel.add(scanEndY, "6, 6");
        panel.add(new LocationButtonsPanel(scanEndX, scanEndY, (JTextField) null,
                (JTextField) null), "8, 6");

        panel.add(new JLabel(Translations.getString(
                "ReferenceAprilTagFeederConfigurationWizard.ScanPanel.StepLabel.text")), //$NON-NLS-1$
                "2, 8, right, default");
        scanStepX = new JTextField(8);
        scanStepY = new JTextField(8);
        panel.add(scanStepX, "4, 8");
        panel.add(scanStepY, "6, 8");

        panel.add(new JLabel(Translations.getString(
                "ReferenceAprilTagFeederConfigurationWizard.ScanPanel.FamilyLabel.text")), //$NON-NLS-1$
                "2, 10, right, default");
        tagFamily = new JComboBox<>(TagFamily.values());
        panel.add(tagFamily, "4, 10, 3, 1, left, default");

        panel.add(new JLabel(Translations.getString(
                "ReferenceAprilTagFeederConfigurationWizard.ScanPanel.ActuatorLabel.text")), //$NON-NLS-1$
                "2, 12, right, default");
        actuatorName = new JTextField(20);
        panel.add(actuatorName, "4, 12, 3, 1, fill, default");

        scanButton = new JButton(scanAction);
        panel.add(scanButton, "8, 12");
        scanStatus = new JLabel();
        panel.add(scanStatus, "2, 14, 7, 1, left, default");
    }

    @Override
    public void createBindings() {
        super.createBindings();

        Converter<Integer, String> nullableIntegerConverter = new Converter<Integer, String>() {
            @Override
            public String convertForward(Integer value) {
                return value == null ? "" : value.toString();
            }

            @Override
            public Integer convertReverse(String value) {
                String trimmedValue = value.trim();
                return trimmedValue.isEmpty() ? null : Integer.valueOf(trimmedValue);
            }
        };
        LengthConverter lengthConverter = new LengthConverter();
        DoubleConverter doubleConverter =
                new DoubleConverter(Configuration.get().getLengthDisplayFormat());

        addWrappedBinding(feeder, "tagId", tagId, "text", nullableIntegerConverter);
        bind(UpdateStrategy.READ, feeder, "presenceText", presence, "text");
        bind(UpdateStrategy.READ, feeder, "tapePartPitch", partPitch, "text", lengthConverter);
        bind(UpdateStrategy.READ, feeder, "rotationInTape", rotationInTape, "text",
                doubleConverter);

        MutableLocationProxy tagLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ, feeder, "location", tagLocation, "location");
        bind(UpdateStrategy.READ, tagLocation, "lengthX", tagX, "text", lengthConverter);
        bind(UpdateStrategy.READ, tagLocation, "lengthY", tagY, "text", lengthConverter);
        bind(UpdateStrategy.READ, tagLocation, "lengthZ", tagZ, "text", lengthConverter);
        bind(UpdateStrategy.READ, tagLocation, "rotation", tagRotation, "text", doubleConverter);

        MutableLocationProxy offset = new MutableLocationProxy();
        addWrappedBinding(feeder, "tagToPickOffset", offset, "location");
        addWrappedBinding(offset, "lengthX", offsetX, "text", lengthConverter);
        addWrappedBinding(offset, "lengthY", offsetY, "text", lengthConverter);
        addWrappedBinding(offset, "lengthZ", offsetZ, "text", lengthConverter);
        addWrappedBinding(offset, "rotation", offsetRotation, "text", doubleConverter);
        bind(UpdateStrategy.READ, tagLocation, "location", offsetLocationButtons, "baseLocation");

        MutableLocationProxy scanStart = new MutableLocationProxy();
        addWrappedBinding(properties, "scanStartLocation", scanStart, "location");
        addWrappedBinding(scanStart, "lengthX", scanStartX, "text", lengthConverter);
        addWrappedBinding(scanStart, "lengthY", scanStartY, "text", lengthConverter);

        MutableLocationProxy scanEnd = new MutableLocationProxy();
        addWrappedBinding(properties, "scanEndLocation", scanEnd, "location");
        addWrappedBinding(scanEnd, "lengthX", scanEndX, "text", lengthConverter);
        addWrappedBinding(scanEnd, "lengthY", scanEndY, "text", lengthConverter);

        addWrappedBinding(properties, "scanStepX", scanStepX, "text", lengthConverter);
        addWrappedBinding(properties, "scanStepY", scanStepY, "text", lengthConverter);
        addWrappedBinding(properties, "tagFamily", tagFamily, "selectedItem");
        addWrappedBinding(properties, "actuatorName", actuatorName, "text");

        ComponentDecorators.decorateWithAutoSelect(tagId);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(offsetX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(offsetY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(offsetZ);
        ComponentDecorators.decorateWithAutoSelect(offsetRotation);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(scanStartX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(scanStartY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(scanEndX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(scanEndY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(scanStepX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(scanStepY);
        ComponentDecorators.decorateWithAutoSelect(actuatorName);
    }

    private final Action scanAction = new AbstractAction(Translations.getString(
            "ReferenceAprilTagFeederConfigurationWizard.ScanPanel.ScanButton.text")) { //$NON-NLS-1$
        @Override
        public void actionPerformed(ActionEvent event) {
            saveToModel();
            scanButton.setEnabled(false);
            scanStatus.setText(Translations.getString(
                    "ReferenceAprilTagFeederConfigurationWizard.ScanPanel.ScanningLabel.text")); //$NON-NLS-1$

            UiUtils.submitUiMachineTask(ReferenceAprilTagFeeder::scan, count -> {
                scanButton.setEnabled(true);
                scanStatus.setText(String.format(Translations.getString(
                        "ReferenceAprilTagFeederConfigurationWizard.ScanPanel.FoundLabel.text"), //$NON-NLS-1$
                        count));
            }, throwable -> {
                scanButton.setEnabled(true);
                scanStatus.setText(Translations.getString(
                        "ReferenceAprilTagFeederConfigurationWizard.ScanPanel.FailedLabel.text")); //$NON-NLS-1$
                UiUtils.showError(throwable);
            });
        }
    };
}
