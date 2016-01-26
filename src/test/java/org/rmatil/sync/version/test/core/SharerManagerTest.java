package org.rmatil.sync.version.test.core;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.rmatil.sync.persistence.api.IStorageAdapter;
import org.rmatil.sync.persistence.core.local.LocalStorageAdapter;
import org.rmatil.sync.persistence.exceptions.InputOutputException;
import org.rmatil.sync.version.api.AccessType;
import org.rmatil.sync.version.api.PathType;
import org.rmatil.sync.version.core.ObjectManager;
import org.rmatil.sync.version.core.SharerManager;
import org.rmatil.sync.version.core.model.PathObject;
import org.rmatil.sync.version.core.model.Sharer;
import org.rmatil.sync.version.core.model.Version;
import org.rmatil.sync.version.test.config.Config;
import org.rmatil.sync.version.test.util.FileUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.*;

public class SharerManagerTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    public static final Path ROOT_TEST_DIR = Config.DEFAULT.getRootTestDir();

    protected static IStorageAdapter storageAdapter;

    protected static ObjectManager objectManager;

    protected static SharerManager sharerManager;

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
        sharerManager = new SharerManager(objectManager);

        Sharer sharer1 = new Sharer("192.168.1.1", 80, AccessType.READ);
        Sharer sharer2 = new Sharer("192.168.3.2", 80, AccessType.WRITE);

        Set<Sharer> sharers = new HashSet<>();
        sharers.add(sharer1);
        sharers.add(sharer2);

        List<Version> versions = new ArrayList<>();

        pathObject = new PathObject("myFile.txt", null, "somePath/to/dir", PathType.FILE, true, true, sharers, versions);

        objectManager.writeObject(pathObject);
    }

    @AfterClass
    public static void tearDown() {
        FileUtil.delete(ROOT_TEST_DIR.toFile());
    }

    @Test
    public void testModifySharer()
            throws InputOutputException {
        Set<Sharer> sharers = sharerManager.getSharer(pathObject.getAbsolutePath());
        assertFalse("sharers should not be empty", sharers.isEmpty());
        assertEquals("There should be 2 sharers", 2, sharers.size());

        Sharer s1 = new Sharer("127.0.0.1", 1234, AccessType.READ);
        sharerManager.addSharer(s1, pathObject.getAbsolutePath());

        Set<Sharer> sharers1 = sharerManager.getSharer(pathObject.getAbsolutePath());
        assertEquals("sharer do not contain s1", 3, sharers1.size());
        assertThat("sharers should contain the new sharer", sharers1, hasItem(s1));

        sharerManager.removeSharer(s1, pathObject.getAbsolutePath());

        Set<Sharer> sharers2 = sharerManager.getSharer(pathObject.getAbsolutePath());
        assertEquals("versions should contain 2 element again after removing", 2, sharers2.size());
    }

    @Test
    public void testAccessor() {
        assertEquals("ObjectManager is not the same", objectManager, sharerManager.getObjectManager());
    }
}
