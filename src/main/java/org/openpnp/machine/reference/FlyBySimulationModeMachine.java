/*
 * Copyright (C) 2026 OpenPnP contributors
 */
package org.openpnp.machine.reference;

import java.util.ArrayList;
import java.util.List;

import org.openpnp.machine.reference.camera.FlyBySimulatedUpCamera;
import org.openpnp.machine.reference.driver.VirtualFlyByDriver;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Driver;
import org.simpleframework.xml.Root;

/**
 * SimulationModeMachine variant with Fly-By-capable virtual camera and driver choices.
 *
 * This lets a user configure and run Fly-By jobs entirely in OpenPnP without camera,
 * controller, motors or firmware connected.
 */
@Root
public class FlyBySimulationModeMachine extends SimulationModeMachine {
    @Override
    public List<Class<? extends Camera>> getCompatibleCameraClasses() {
        List<Class<? extends Camera>> classes = new ArrayList<>(super.getCompatibleCameraClasses());
        if (!classes.contains(FlyBySimulatedUpCamera.class)) {
            classes.add(FlyBySimulatedUpCamera.class);
        }
        return classes;
    }

    @Override
    public List<Class<? extends Driver>> getCompatibleDriverClasses() {
        List<Class<? extends Driver>> classes = new ArrayList<>(super.getCompatibleDriverClasses());
        if (!classes.contains(VirtualFlyByDriver.class)) {
            classes.add(VirtualFlyByDriver.class);
        }
        return classes;
    }
}
