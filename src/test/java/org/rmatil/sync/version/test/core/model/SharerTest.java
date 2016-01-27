package org.rmatil.sync.version.test.core.model;

import org.junit.Before;
import org.junit.Test;
import org.rmatil.sync.version.api.AccessType;
import org.rmatil.sync.version.core.model.Sharer;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class SharerTest {

    protected static Sharer sharer;
    protected static Sharer sharer2;

    protected static final String USERNAME = "Pelican Steve";

    protected static List<String> list;

    @Before
    public void before() {
        list = new ArrayList<>();
        list.add("Version1");

        List<String> list2 = new ArrayList<>();
        list2.add("Version1");
        sharer = new Sharer(USERNAME, AccessType.WRITE, list);
        sharer2 = new Sharer(USERNAME, AccessType.WRITE, list2);
    }

    @Test
    public void testAccessor() {
        assertEquals("Username not equal", USERNAME, sharer.getUsername());
        sharer.setUsername("Shequondolisa Bivouac");
        assertEquals("Username not equal after change", "Shequondolisa Bivouac", sharer.getUsername());
        assertEquals("AccessType not equal", AccessType.WRITE, sharer.getAccessType());
        sharer.setAccessType(AccessType.READ);
        assertEquals("AccessType not equal after changing", AccessType.READ, sharer.getAccessType());

        assertArrayEquals("Sharing history should not be empty", list.toArray(), sharer.getSharingHistory().toArray());
    }

    @Test
    public void testEquals() {
        assertTrue("Sharer should be equals", sharer.equals(sharer2));

        Sharer sharer3 = new Sharer(USERNAME, AccessType.WRITE, new ArrayList<>());

        assertFalse("Sharer3 should not be equal", sharer.equals(sharer3));
    }
}
