package org.rmatil.sync.version.test.core.model;

import org.junit.BeforeClass;
import org.junit.Test;
import org.rmatil.sync.version.api.AccessType;
import org.rmatil.sync.version.api.PathType;
import org.rmatil.sync.version.core.model.PathObject;

import static org.junit.Assert.*;

public class PathObjectTest {

    protected static final String  NAME       = "myFile.txt";
    protected static final String  PATH       = "";
    protected static final String  OWNER      = "Owner's Username";
    protected static final boolean IS_SHARED  = false;
    protected static final boolean IS_DELETED = false;
    protected static final AccessType ACCESS_TYPE = AccessType.WRITE;

    protected static PathObject pathObject;

    @BeforeClass
    public static void setUp() {
        pathObject = new PathObject(NAME, PATH, PathType.DIRECTORY, ACCESS_TYPE, IS_SHARED, IS_DELETED, OWNER, null, null);
    }

    @Test
    public void testAccessor() {
        assertEquals("Absolute path is not equals", NAME, pathObject.getAbsolutePath());
        assertEquals("PathType is not equal", PathType.DIRECTORY, pathObject.getPathType());
        assertFalse("PathObject is not not shared", pathObject.isShared());
        pathObject.setIsShared(true);
        assertTrue("PathObject should be shared after sharing", pathObject.isShared());
        assertEquals("Sharers are not empty", 0, pathObject.getSharers().size());
        assertEquals("Versions are not empty", 0, pathObject.getVersions().size());

        assertEquals("Access type should be equal", ACCESS_TYPE, pathObject.getAccessType());
        pathObject.setAccessType(AccessType.READ);
        assertEquals("Access type should be equal after writing", AccessType.READ, pathObject.getAccessType());
    }
}
