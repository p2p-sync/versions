package org.rmatil.sync.version.test.core.model;

import org.junit.BeforeClass;
import org.junit.Test;
import org.rmatil.sync.version.api.AccessType;
import org.rmatil.sync.version.core.model.Sharer;

import static org.junit.Assert.*;

public class SharerTest {

    protected static Sharer sharer;

    protected static final String ADDRESS = "192.168.1.1";

    protected static final int PORT = 4003;

    @BeforeClass
    public static void setUp() {
        sharer = new Sharer(ADDRESS, PORT, AccessType.WRITE);
    }

    @Test
    public void testAccessor() {
        assertEquals("Address not equal", ADDRESS, sharer.getAddress());
        sharer.setAddress("123");
        assertEquals("Address not equal after change", "123", sharer.getAddress());
        assertEquals("Port is not equal", PORT, sharer.getPort());
        sharer.setPort(89);
        assertEquals("Port is not equal after change", 89, sharer.getPort());
        assertEquals("AccessType not equal", AccessType.WRITE, sharer.getAccessType());
        sharer.setAccessType(AccessType.READ);
        assertEquals("AccessType not equal after changing", AccessType.READ, sharer.getAccessType());

    }
}
