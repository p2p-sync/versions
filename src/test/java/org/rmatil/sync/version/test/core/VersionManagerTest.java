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
import org.rmatil.sync.version.core.VersionManager;
import org.rmatil.sync.version.core.model.PathObject;
import org.rmatil.sync.version.core.model.Sharer;
import org.rmatil.sync.version.core.model.Version;
import org.rmatil.sync.version.test.config.Config;
import org.rmatil.sync.version.test.util.FileUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class VersionManagerTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    public static final Path ROOT_TEST_DIR = Config.DEFAULT.getRootTestDir();

    protected static IStorageAdapter storageAdapter;

    protected static ObjectManager objectManager;

    protected static VersionManager versionManager;

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
        versionManager = new VersionManager(objectManager);

        Sharer sharer1 = new Sharer("192.168.1.1", AccessType.READ);
        Sharer sharer2 = new Sharer("192.168.3.2", AccessType.WRITE);

        List<Sharer> sharers = new ArrayList<Sharer>();
        sharers.add(sharer1);
        sharers.add(sharer2);

        List<Version> versions = new ArrayList<Version>();

        pathObject = new PathObject("myFile.txt", "somePath/to/dir", PathType.FILE, true, sharers, versions);

        objectManager.writeObject(pathObject);
    }

    @AfterClass
    public static void tearDown() {
        FileUtil.delete(ROOT_TEST_DIR.toFile());
    }

    @Test
    public void testModifyVersion()
            throws InputOutputException {
        List<Version> versions = versionManager.getVersions(pathObject.getAbsolutePath());
        assertTrue("versions are not empty", versions.isEmpty());

        Version v1 = new Version("hashOfVersion1");
        versionManager.addVersion(v1, pathObject.getAbsolutePath());

        List<Version> versions1 = versionManager.getVersions(pathObject.getAbsolutePath());
        assertEquals("versions do not contain v1", 1, versions1.size());
        assertEquals("v1 is not equal", v1, versions1.get(0));

        versionManager.removeVersion(v1, pathObject.getAbsolutePath());

        List<Version> versions2 = versionManager.getVersions(pathObject.getAbsolutePath());
        assertTrue("versions are not empty after removing", versions2.isEmpty());
    }

    @Test
    public void testAccessor() {
        assertEquals("ObjectManager is not the same", objectManager, versionManager.getObjectManager());
    }
}
