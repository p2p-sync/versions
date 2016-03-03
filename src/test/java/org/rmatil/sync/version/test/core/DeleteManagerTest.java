package org.rmatil.sync.version.test.core;

import org.junit.*;
import org.junit.rules.ExpectedException;
import org.rmatil.sync.persistence.api.IStorageAdapter;
import org.rmatil.sync.persistence.core.local.LocalStorageAdapter;
import org.rmatil.sync.persistence.exceptions.InputOutputException;
import org.rmatil.sync.version.api.AccessType;
import org.rmatil.sync.version.api.DeleteType;
import org.rmatil.sync.version.api.PathType;
import org.rmatil.sync.version.core.DeleteManager;
import org.rmatil.sync.version.core.ObjectManager;
import org.rmatil.sync.version.core.model.Delete;
import org.rmatil.sync.version.core.model.PathObject;
import org.rmatil.sync.version.test.config.Config;
import org.rmatil.sync.version.test.util.FileUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DeleteManagerTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    public static final Path ROOT_TEST_DIR = Config.DEFAULT.getRootTestDir();

    protected static IStorageAdapter storageAdapter;

    protected static ObjectManager objectManager;

    protected static DeleteManager deleteManager;

    protected static PathObject pathObject;

    @BeforeClass
    public static void setUp()
            throws InputOutputException {
        try {
            // create test dir
            if (! Files.exists(ROOT_TEST_DIR)) {
                Files.createDirectory(ROOT_TEST_DIR);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        storageAdapter = new LocalStorageAdapter(ROOT_TEST_DIR);
        objectManager = new ObjectManager("index.json", "objects", storageAdapter);
        deleteManager = new DeleteManager(objectManager);

        pathObject = new PathObject("myFile.txt", "somePath/to/dir", PathType.FILE, AccessType.WRITE, true, new Delete(DeleteType.EXISTENT, new ArrayList<>()), null, new HashSet<>(), new ArrayList<>());
        objectManager.writeObject(pathObject);
    }

    @AfterClass
    public static void tearDown() {
        FileUtil.delete(ROOT_TEST_DIR.toFile());
    }

    @Before
    public void before()
            throws InputOutputException {
        // reset path object
        objectManager.writeObject(pathObject);
    }

    @Test
    public void testGetDelete()
            throws InputOutputException {
        Delete delete = deleteManager.getDelete("somePath/to/dir/myFile.txt");

        assertNotNull("Delete should not be null", delete);
        assertEquals("File should be existent", DeleteType.EXISTENT, delete.getDeleteType());
        assertEquals("Delete history should be empty", 0, delete.getDeleteHistory().size());
    }

    @Test
    public void testAccessors()
            throws InputOutputException {
        Delete delete = deleteManager.getDelete("somePath/to/dir/myFile.txt");

        assertNotNull("Delete should not be null", delete);
        assertEquals("File should be existent", DeleteType.EXISTENT, delete.getDeleteType());
        assertEquals("Delete history should be empty", 0, delete.getDeleteHistory().size());

        deleteManager.setIsDeleted("somePath/to/dir/myFile.txt");

        delete = deleteManager.getDelete("somePath/to/dir/myFile.txt");

        assertNotNull("Delete should not be null", delete);
        assertEquals("File should be deleted", DeleteType.DELETED, delete.getDeleteType());
        assertEquals("Delete history should have 1 entry", 1, delete.getDeleteHistory().size());

        deleteManager.setIsExistent("somePath/to/dir/myFile.txt");

        delete = deleteManager.getDelete("somePath/to/dir/myFile.txt");

        assertNotNull("Delete should not be null", delete);
        assertEquals("File should be existent again", DeleteType.EXISTENT, delete.getDeleteType());
        assertEquals("Delete history should have 2 entry", 2, delete.getDeleteHistory().size());
    }

}
