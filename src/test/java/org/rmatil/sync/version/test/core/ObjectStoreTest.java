package org.rmatil.sync.version.test.core;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.rmatil.sync.commons.hashing.Hash;
import org.rmatil.sync.persistence.api.IStorageAdapter;
import org.rmatil.sync.persistence.core.local.LocalStorageAdapter;
import org.rmatil.sync.persistence.exceptions.InputOutputException;
import org.rmatil.sync.version.api.PathType;
import org.rmatil.sync.version.core.ObjectStore;
import org.rmatil.sync.version.core.model.Index;
import org.rmatil.sync.version.core.model.PathObject;
import org.rmatil.sync.version.test.config.Config;
import org.rmatil.sync.version.test.util.APathTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ObjectStoreTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    protected static ObjectStore objectStore;

    protected static IStorageAdapter storageAdapter;

    protected static final Path ROOT_TEST_DIR = Config.DEFAULT.getRootTestDir();

    protected static Path testFile = ROOT_TEST_DIR.resolve("myFile.txt");

    protected static Path testDir = ROOT_TEST_DIR.resolve("myDir");

    @BeforeClass
    public static void setUp()
            throws InputOutputException, IOException {
        APathTest.setUp();

        if (! Files.exists(ROOT_TEST_DIR.resolve(".sync"))) {
            Files.createDirectory(ROOT_TEST_DIR.resolve(".sync"));
        }

        storageAdapter = new LocalStorageAdapter(ROOT_TEST_DIR.resolve(".sync"));
        objectStore = new ObjectStore(ROOT_TEST_DIR, "index.json", "object", storageAdapter);
    }

    @AfterClass
    public static void tearDown() {
        APathTest.tearDown();
    }

    @Test
    public void testOnCreateFile()
            throws IOException, InterruptedException, InputOutputException {
        if (! Files.exists(testFile)) {
            Files.createFile(testFile);
        }

        // wait a bit for file creation
        Thread.sleep(100L);

        objectStore.onCreateFile(ROOT_TEST_DIR.relativize(testFile).toString(), "myHash");

        PathObject pathObject = objectStore.getObjectManager().getObject(Hash.hash(Config.DEFAULT.getHashingAlgorithm(), testFile.getFileName().toString()));

        assertEquals("Name is not equal", testFile.getFileName().toString(), pathObject.getName());
        assertEquals("Versions are not present", 1, pathObject.getVersions().size());
        assertEquals("Hash is not equal", "myHash", pathObject.getVersions().get(0).getHash());
        assertEquals("PathType is not a file ", PathType.FILE, pathObject.getPathType());

        assertTrue("Index does not contain file", objectStore.getObjectManager().getIndex().getPaths().containsKey(ROOT_TEST_DIR.relativize(testFile).toString()));
    }

    @Test
    public void testOnCreateDir()
            throws IOException, InterruptedException, InputOutputException {

        if (! Files.exists(testDir)) {
            Files.createDirectory(testDir);
        }

        // wait a bit for file creation
        Thread.sleep(100L);

        objectStore.onCreateFile(ROOT_TEST_DIR.relativize(testDir).toString(), null);

        PathObject pathObject = objectStore.getObjectManager().getObject(Hash.hash(Config.DEFAULT.getHashingAlgorithm(), testDir.getFileName().toString()));

        assertEquals("Name is not equal", testDir.getFileName().toString(), pathObject.getName());
        assertEquals("Versions are not present", 1, pathObject.getVersions().size());
        assertEquals("Hash is not equal", null, pathObject.getVersions().get(0).getHash());
        assertEquals("PathType is not a directory", PathType.DIRECTORY, pathObject.getPathType());

        assertTrue("Index does not contain file", objectStore.getObjectManager().getIndex().getPaths().containsKey(ROOT_TEST_DIR.relativize(testDir).toString()));
    }

    @Test
    public void testOnModifyFile()
            throws IOException, InterruptedException, InputOutputException {
        if (! Files.exists(testFile)) {
            Files.createFile(testFile);

            // wait a bit for file creation
            Thread.sleep(100L);
        }

        objectStore.onCreateFile(ROOT_TEST_DIR.relativize(testFile).toString(), "myHash");

        Thread.sleep(100L);

        objectStore.onModifyFile(ROOT_TEST_DIR.relativize(testFile).toString(), "myHash2");

        PathObject pathObject = objectStore.getObjectManager().getObject(Hash.hash(Config.DEFAULT.getHashingAlgorithm(), testFile.getFileName().toString()));

        assertEquals("Name is not equal", testFile.getFileName().toString(), pathObject.getName());
        assertEquals("Versions are not present", 2, pathObject.getVersions().size());
        assertEquals("Hash is not equal", "myHash2", pathObject.getVersions().get(1).getHash());
        assertEquals("PathType is not a file ", PathType.FILE, pathObject.getPathType());
    }

    @Test
    public void testOnRemoveFile()
            throws IOException, InterruptedException, InputOutputException {
        if (! Files.exists(testFile)) {
            Files.createFile(testFile);
        }

        if (! Files.exists(testDir)) {
            Files.createDirectory(testDir);
        }

        if (! Files.exists(testDir.resolve("myOtherFile.txt"))) {
            Files.createFile(testDir.resolve("myOtherFile.txt"));
        }

        // wait a bit for file creation
        Thread.sleep(100L);

        objectStore.onCreateFile(ROOT_TEST_DIR.relativize(testFile).toString(), "myHash");
        objectStore.onCreateFile(ROOT_TEST_DIR.relativize(testDir).toString(), null);
        objectStore.onCreateFile(ROOT_TEST_DIR.relativize(testDir.resolve("myOtherFile.txt")).toString(), "myHash2");

        Thread.sleep(100L);

        objectStore.onRemoveFile(ROOT_TEST_DIR.relativize(testDir.resolve("myOtherFile.txt")).toString());
        objectStore.onRemoveFile(ROOT_TEST_DIR.relativize(testDir).toString());
        objectStore.onRemoveFile(ROOT_TEST_DIR.relativize(testFile).toString());

        Thread.sleep(100L);

        System.err.println(objectStore.getObjectManager().getIndex().getPaths());
        assertTrue("Entries in index are not empty", objectStore.getObjectManager().getIndex().getPaths().entrySet().isEmpty());
        thrown.expect(InputOutputException.class);
        objectStore.getObjectManager().getObject(Hash.hash(Config.DEFAULT.getHashingAlgorithm(), ROOT_TEST_DIR.relativize(testDir.resolve("myOtherFile.txt")).toString()));
        thrown.expect(InputOutputException.class);
        objectStore.getObjectManager().getObject(Hash.hash(Config.DEFAULT.getHashingAlgorithm(), ROOT_TEST_DIR.relativize(testDir).toString()));
        thrown.expect(InputOutputException.class);
        objectStore.getObjectManager().getObject(Hash.hash(Config.DEFAULT.getHashingAlgorithm(), ROOT_TEST_DIR.relativize(testFile).toString()));
    }

    @Test
    public void testOnMove()
            throws IOException, InputOutputException, InterruptedException {
        if (! Files.exists(testDir)) {
            Files.createDirectory(testDir);
        }

        if (! Files.exists(testDir.resolve("myOtherFile.txt"))) {
            Files.createFile(testDir.resolve("myOtherFile.txt"));
        }

        if (! Files.exists(ROOT_TEST_DIR.resolve(Paths.get("otherDir")))) {
            Files.createDirectory(ROOT_TEST_DIR.resolve(Paths.get("otherDir")));
        }

        // wait a bit for file creation
        Thread.sleep(100L);

        objectStore.onCreateFile(ROOT_TEST_DIR.relativize(testDir).toString(), null);
        objectStore.onCreateFile(ROOT_TEST_DIR.relativize(testDir).resolve("myOtherFile.txt").toString(), "myHash2");

        Thread.sleep(100L);

        Files.move(testDir, ROOT_TEST_DIR.resolve(Paths.get("otherDir")).resolve(testDir.getFileName()));

        Thread.sleep(500L);

        objectStore.onMoveFile(ROOT_TEST_DIR.relativize(testDir.resolve("myOtherFile.txt")).toString(), ROOT_TEST_DIR.resolve(Paths.get("otherDir")).resolve(testDir.getFileName()).resolve("myOtherFile.txt").toString());
        objectStore.onMoveFile(ROOT_TEST_DIR.relativize(testDir).toString(), ROOT_TEST_DIR.resolve(Paths.get("otherDir")).resolve(testDir.getFileName()).toString());

        assertTrue(objectStore.getObjectManager().getIndex().getPaths().containsKey(ROOT_TEST_DIR.resolve(Paths.get("otherDir")).resolve(testDir.getFileName()).resolve("myOtherFile.txt").toString()));
        assertTrue(objectStore.getObjectManager().getIndex().getPaths().containsKey(ROOT_TEST_DIR.resolve(Paths.get("otherDir")).resolve(testDir.getFileName()).toString()));
    }

    @Test
    public void testSync()
            throws IOException, InterruptedException, InputOutputException {
        if (! Files.exists(testDir)) {
            Files.createDirectory(testDir);
        }

        if (! Files.exists(testDir.resolve("myOtherFile.txt"))) {
            Files.createFile(testDir.resolve("myOtherFile.txt"));
        }

        if (! Files.exists(ROOT_TEST_DIR.resolve(Paths.get("otherDir")))) {
            Files.createDirectory(ROOT_TEST_DIR.resolve(Paths.get("otherDir")));
        }

        // wait a bit for file creation
        Thread.sleep(100L);

        objectStore.getObjectManager().clear();

        // wait for all files to be deleted
        Thread.sleep(200L);

        objectStore.sync(ROOT_TEST_DIR.toFile());

        Index index = objectStore.getObjectManager().getIndex();

        String key1 = Paths.get("myDir").toString();
        String key2 = Paths.get("myDir").resolve("myOtherFile.txt").toString();
        String key3 = Paths.get("otherDir").toString();

        assertTrue(index.getPaths().containsKey(key1));
        assertTrue(index.getPaths().containsKey(key2));
        assertTrue(index.getPaths().containsKey(key3));

        // should not throw an exception
        objectStore.getObjectManager().getObject(Hash.hash(Config.DEFAULT.getHashingAlgorithm(), key1));
        objectStore.getObjectManager().getObject(Hash.hash(Config.DEFAULT.getHashingAlgorithm(), key2));
        objectStore.getObjectManager().getObject(Hash.hash(Config.DEFAULT.getHashingAlgorithm(), key3));

        objectStore.getObjectManager().clear();
    }
}
