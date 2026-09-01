package org.openpnp.gui.support;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.UUID;
import java.util.prefs.Preferences;

import javax.swing.JTable;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class TableUtilsTest {
    private final Preferences preferences = Preferences.userRoot().node(
            "/org/openpnp/test/TableUtilsTest/" + UUID.randomUUID());

    @AfterEach
    public void removePreferences() throws Exception {
        preferences.removeNode();
    }

    @Test
    public void restoresSavedColumnOrder() {
        JTable source = createTable();
        TableUtils.installColumnWidthSavers(source, preferences, "table");
        source.moveColumn(0, 2);

        JTable restored = createTable();
        TableUtils.installColumnWidthSavers(restored, preferences, "table");

        assertArrayEquals(new int[] {1, 2, 0}, modelIndices(restored));
    }

    @Test
    public void ignoresInvalidSavedColumnOrder() {
        preferences.putInt("table.order.0", 1);
        preferences.putInt("table.order.1", 1);
        preferences.putInt("table.order.2", 0);

        JTable table = createTable();
        TableUtils.installColumnWidthSavers(table, preferences, "table");

        assertArrayEquals(new int[] {0, 1, 2}, modelIndices(table));
    }

    private static JTable createTable() {
        return new JTable(new Object[0][3], new Object[] {"A", "B", "C"});
    }

    private static int[] modelIndices(JTable table) {
        int[] indices = new int[table.getColumnCount()];
        for (int viewIndex = 0; viewIndex < table.getColumnCount(); viewIndex++) {
            indices[viewIndex] = table.getColumnModel().getColumn(viewIndex).getModelIndex();
        }
        return indices;
    }
}