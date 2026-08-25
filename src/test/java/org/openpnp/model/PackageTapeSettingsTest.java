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

package org.openpnp.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.StringWriter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.simpleframework.xml.Serializer;

import com.google.common.io.Files;

public class PackageTapeSettingsTest {
    @BeforeEach
    public void setUp() throws Exception {
        File workingDirectory = new File(Files.createTempDir(), ".openpnp");
        Configuration.initialize(workingDirectory);
        Configuration.get().load();
    }

    @Test
    public void tapeSettingsHaveUsefulDefaults() {
        Package pkg = new Package("TEST");

        assertEquals(new Length(4, LengthUnit.Millimeters), pkg.getTapePartPitch());
        assertEquals(0.0, pkg.getRotationInTape());
    }

    @Test
    public void tapeSettingsSurviveSerialization() throws Exception {
        Package pkg = new Package("TEST");
        pkg.setTapePartPitch(new Length(2, LengthUnit.Millimeters));
        pkg.setRotationInTape(90.0);

        Serializer serializer = Configuration.createSerializer();
        StringWriter writer = new StringWriter();
        serializer.write(pkg, writer);
        String xml = writer.toString();

        assertTrue(xml.contains("tape-part-pitch"));
        assertTrue(xml.contains("rotation-in-tape=\"90.0\""));

        Package restored = serializer.read(Package.class, xml);
        assertEquals(pkg.getTapePartPitch(), restored.getTapePartPitch());
        assertEquals(pkg.getRotationInTape(), restored.getRotationInTape());
    }
}
