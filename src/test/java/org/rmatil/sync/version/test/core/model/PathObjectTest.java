package org.rmatil.sync.version.test.core.model;

import org.junit.BeforeClass;
import org.junit.Test;
import org.rmatil.sync.version.api.PathType;
import org.rmatil.sync.version.core.model.PathObject;

import java.util.UUID;

import static org.junit.Assert.*;

public class PathObjectTest {

    protected static final String  NAME       = "myFile.txt";
    protected static final UUID    FILE_ID    = UUID.randomUUID();
    protected static final String  PATH       = "";
    protected static final boolean IS_SHARED  = false;
    protected static final boolean IS_DELETED = false;

    protected static PathObject pathObject;

    @BeforeClass
    public static void setUp() {
        pathObject = new PathObject(NAME, FILE_ID, PATH, PathType.DIRECTORY, IS_SHARED, IS_DELETED, null, null);
    }

    @Test
    public void testAccessor() {
        assertEquals("Absolute path is not equals", NAME, pathObject.getAbsolutePath());
        assertEquals("FileId is not equals", FILE_ID, pathObject.getFileId());
        assertEquals("PathType is not equal", PathType.DIRECTORY, pathObject.getPathType());
        assertFalse("PathObject is not not shared", pathObject.isShared());
        assertEquals("Sharers are not empty", 0, pathObject.getSharers().size());
        assertEquals("Versions are not empty", 0, pathObject.getVersions().size());
    }


}
