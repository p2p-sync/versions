package org.rmatil.sync.version.test.core.model;

import org.junit.BeforeClass;
import org.junit.Test;
import org.rmatil.sync.version.core.model.Index;

import java.util.HashMap;
import java.util.UUID;

import static org.junit.Assert.*;

public class IndexTest {

    protected static final String PATH_TO_FILE = "path/to/file.txt";

    protected static final String HASH_OF_FILE_PATH = "hashOfFilePath";

    protected static Index index;

    @BeforeClass
    public static void setUp() {
        index = new Index(new HashMap<>());
    }

    @Test
    public void testAccessors() {
        index.addPath(PATH_TO_FILE, HASH_OF_FILE_PATH);
        assertEquals("PathIdentifier is not added", 1, index.getPaths().size());
        index.removePath(PATH_TO_FILE);
        assertEquals("PathIdentifier is not removed", 0, index.getPaths().size());
    }
}
