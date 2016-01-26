package org.rmatil.sync.version.test.core.model;

import org.junit.BeforeClass;
import org.junit.Test;
import org.rmatil.sync.version.api.AccessType;
import org.rmatil.sync.version.core.model.Sharer;

import static org.junit.Assert.*;

public class SharerTest {

    protected static Sharer sharer;

    protected static final String USERNAME = "Pelican Steve";

    @BeforeClass
    public static void setUp() {
        sharer = new Sharer(USERNAME, AccessType.WRITE);
    }

    @Test
    public void testAccessor() {
        assertEquals("Username not equal", USERNAME, sharer.getUsername());
        sharer.setUsername("Shequondolisa Bivouac");
        assertEquals("Username not equal after change", "Shequondolisa Bivouac", sharer.getUsername());
        assertEquals("AccessType not equal", AccessType.WRITE, sharer.getAccessType());
        sharer.setAccessType(AccessType.READ);
        assertEquals("AccessType not equal after changing", AccessType.READ, sharer.getAccessType());

    }
}
