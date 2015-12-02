package org.rmatil.sync.version.test.core.model;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.junit.BeforeClass;
import org.junit.Test;
import org.rmatil.sync.version.core.model.Version;

import static org.junit.Assert.*;

public class VersionTest {

    protected static final String HASH = "hash";

    protected static Version version;

    @BeforeClass
    public static void setUp() {
        version = new Version(HASH);
    }

    @Test
    public void testHash() {
        int hashCode = new HashCodeBuilder(17, 31).append(HASH).toHashCode();

        assertEquals("HashCode is not the same", hashCode, version.hashCode());
    }

    @Test
    public void testEquals() {
        Object obj = new Object();

        assertFalse("Object should not be instance of Version", version.equals(obj));
        assertTrue("Version should be equal to itself", version.equals(version));
    }
}
