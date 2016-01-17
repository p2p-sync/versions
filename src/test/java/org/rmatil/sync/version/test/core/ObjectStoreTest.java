package org.rmatil.sync.version.test.core;

import org.junit.*;
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
import java.util.HashMap;
import java.util.Set;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.*;

public class ObjectStoreTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    protected static ObjectStore objectStore1;
    protected static ObjectStore objectStore2;

    protected static IStorageAdapter storageAdapter1;
    protected static IStorageAdapter storageAdapter2;

    protected static final Path ROOT_TEST_DIR = Config.DEFAULT.getRootTestDir();

    protected static Path testFile = ROOT_TEST_DIR.resolve("myFile.txt");

    protected static Path testDir = ROOT_TEST_DIR.resolve("myDir");

    @BeforeClass
    public static void setUp()
            throws InputOutputException, IOException {
        APathTest.setUp();

        if (! Files.exists(ROOT_TEST_DIR.resolve("sync1"))) {
            Files.createDirectory(ROOT_TEST_DIR.resolve("sync1"));
        }

        if (! Files.exists(ROOT_TEST_DIR.resolve("sync2"))) {
            Files.createDirectory(ROOT_TEST_DIR.resolve("sync2"));
        }

        if (! Files.exists(ROOT_TEST_DIR.resolve("sync1/.sync"))) {
            Files.createDirectory(ROOT_TEST_DIR.resolve("sync1/.sync"));
        }

        if (! Files.exists(ROOT_TEST_DIR.resolve("sync2/.sync"))) {
            Files.createDirectory(ROOT_TEST_DIR.resolve("sync2/.sync"));
        }

        storageAdapter1 = new LocalStorageAdapter(ROOT_TEST_DIR.resolve("sync1/.sync"));
        storageAdapter2 = new LocalStorageAdapter(ROOT_TEST_DIR.resolve("sync2/.sync"));

        objectStore1 = new ObjectStore(ROOT_TEST_DIR, "index.json", "object", storageAdapter1);
        objectStore2 = new ObjectStore(ROOT_TEST_DIR, "index.json", "object", storageAdapter2);
    }

    @AfterClass
    public static void tearDown() {
        APathTest.tearDown();
    }

    @Before
    public void before()
            throws InputOutputException {
        objectStore1.getObjectManager().clear();
        objectStore2.getObjectManager().clear();
    }

    @Test
    public void testOnCreateFile()
            throws IOException, InterruptedException, InputOutputException {
        if (! Files.exists(testFile)) {
            Files.createFile(testFile);
        }

        // wait a bit for file creation
        Thread.sleep(100L);

        objectStore1.onCreateFile(ROOT_TEST_DIR.relativize(testFile).toString(), "myHash");

        PathObject pathObject = objectStore1.getObjectManager().getObject(Hash.hash(Config.DEFAULT.getHashingAlgorithm(), testFile.getFileName().toString()));

        assertEquals("Name is not equal", testFile.getFileName().toString(), pathObject.getName());
        assertEquals("Versions are not present", 1, pathObject.getVersions().size());
        assertEquals("Hash is not equal", "myHash", pathObject.getVersions().get(0).getHash());
        assertEquals("PathType is not a file ", PathType.FILE, pathObject.getPathType());

        assertTrue("Index does not contain file", objectStore1.getObjectManager().getIndex().getPathIdentifiers().containsKey(ROOT_TEST_DIR.relativize(testFile).toString()));
    }

    @Test
    public void testOnCreateDir()
            throws IOException, InterruptedException, InputOutputException {

        if (! Files.exists(testDir)) {
            Files.createDirectory(testDir);
        }

        // wait a bit for file creation
        Thread.sleep(100L);

        objectStore1.onCreateFile(ROOT_TEST_DIR.relativize(testDir).toString(), "someDirHash");

        PathObject pathObject = objectStore1.getObjectManager().getObject(Hash.hash(Config.DEFAULT.getHashingAlgorithm(), testDir.getFileName().toString()));

        assertEquals("Name is not equal", testDir.getFileName().toString(), pathObject.getName());
        assertEquals("Versions are not present", 1, pathObject.getVersions().size());
        assertEquals("Hash is not equal", "someDirHash", pathObject.getVersions().get(0).getHash());
        assertEquals("PathType is not a directory", PathType.DIRECTORY, pathObject.getPathType());

        assertTrue("Index does not contain file", objectStore1.getObjectManager().getIndex().getPathIdentifiers().containsKey(ROOT_TEST_DIR.relativize(testDir).toString()));
    }

    @Test
    public void testOnModifyFile()
            throws IOException, InterruptedException, InputOutputException {
        if (! Files.exists(testFile)) {
            Files.createFile(testFile);

            // wait a bit for file creation
            Thread.sleep(100L);
        }

        objectStore1.onCreateFile(ROOT_TEST_DIR.relativize(testFile).toString(), "myHash");

        Thread.sleep(100L);

        objectStore1.onModifyFile(ROOT_TEST_DIR.relativize(testFile).toString(), "myHash2");

        PathObject pathObject = objectStore1.getObjectManager().getObject(Hash.hash(Config.DEFAULT.getHashingAlgorithm(), testFile.getFileName().toString()));

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

        objectStore1.onCreateFile(ROOT_TEST_DIR.relativize(testFile).toString(), "myHash");
        objectStore1.onCreateFile(ROOT_TEST_DIR.relativize(testDir).toString(), null);
        objectStore1.onCreateFile(ROOT_TEST_DIR.relativize(testDir.resolve("myOtherFile.txt")).toString(), "myHash2");

        Thread.sleep(100L);

        objectStore1.onRemoveFile(ROOT_TEST_DIR.relativize(testDir.resolve("myOtherFile.txt")).toString());
        objectStore1.onRemoveFile(ROOT_TEST_DIR.relativize(testDir).toString());
        objectStore1.onRemoveFile(ROOT_TEST_DIR.relativize(testFile).toString());

        Thread.sleep(100L);

        assertEquals("Entries in index should not be removed", 3, objectStore1.getObjectManager().getIndex().getPaths().entrySet().size());

        PathObject file1 = objectStore1.getObjectManager().getObject(Hash.hash(Config.DEFAULT.getHashingAlgorithm(), ROOT_TEST_DIR.relativize(testDir.resolve("myOtherFile.txt")).toString()));
        assertTrue("File1 should be flagged as deleted", file1.isDeleted());

        PathObject file2 = objectStore1.getObjectManager().getObject(Hash.hash(Config.DEFAULT.getHashingAlgorithm(), ROOT_TEST_DIR.relativize(testDir).toString()));
        assertTrue("File2 should be flagged as deleted", file2.isDeleted());

        PathObject file3 = objectStore1.getObjectManager().getObject(Hash.hash(Config.DEFAULT.getHashingAlgorithm(), ROOT_TEST_DIR.relativize(testFile).toString()));
        assertTrue("File3 should be flagged as deleted", file3.isDeleted());
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

        objectStore1.onCreateFile(ROOT_TEST_DIR.relativize(testDir).toString(), null);
        objectStore1.onCreateFile(ROOT_TEST_DIR.relativize(testDir).resolve("myOtherFile.txt").toString(), "myHash2");

        Thread.sleep(100L);

        Files.move(testDir, ROOT_TEST_DIR.resolve(Paths.get("otherDir")).resolve(testDir.getFileName()));

        Thread.sleep(500L);


        String oldFilePath = ROOT_TEST_DIR.relativize(testDir.resolve("myOtherFile.txt")).toString();
        String newFilePath = ROOT_TEST_DIR.relativize(ROOT_TEST_DIR.resolve(Paths.get("otherDir")).resolve(testDir.getFileName()).resolve("myOtherFile.txt")).toString();
        objectStore1.onMoveFile(oldFilePath, newFilePath);
        String oldDirPath = ROOT_TEST_DIR.relativize(testDir).toString();
        String newDirPath = ROOT_TEST_DIR.relativize(ROOT_TEST_DIR.resolve(Paths.get("otherDir")).resolve(testDir.getFileName())).toString();
        objectStore1.onMoveFile(oldDirPath, newDirPath);

        assertTrue(objectStore1.getObjectManager().getIndex().getPathIdentifiers().containsKey(Paths.get("otherDir").resolve(testDir.getFileName()).resolve("myOtherFile.txt").toString()));
        assertTrue(objectStore1.getObjectManager().getIndex().getPathIdentifiers().containsKey(Paths.get("otherDir").resolve(testDir.getFileName()).toString()));
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

        objectStore1.getObjectManager().clear();

        // wait for all files to be deleted
        Thread.sleep(200L);

        objectStore1.sync(ROOT_TEST_DIR.toFile());

        Index index = objectStore1.getObjectManager().getIndex();

        String key1 = Paths.get("myDir").toString();
        String key2 = Paths.get("myDir").resolve("myOtherFile.txt").toString();
        String key3 = Paths.get("otherDir").toString();

        assertTrue(index.getPathIdentifiers().containsKey(key1));
        assertTrue(index.getPathIdentifiers().containsKey(key2));
        assertTrue(index.getPathIdentifiers().containsKey(key3));

        // should not throw an exception
        objectStore1.getObjectManager().getObject(Hash.hash(Config.DEFAULT.getHashingAlgorithm(), key1));
        objectStore1.getObjectManager().getObject(Hash.hash(Config.DEFAULT.getHashingAlgorithm(), key2));
        objectStore1.getObjectManager().getObject(Hash.hash(Config.DEFAULT.getHashingAlgorithm(), key3));

        objectStore1.getObjectManager().clear();
    }

    @Test
    public void testMergeObjectStore()
            throws InputOutputException, IOException {

        // create some files and directories, create files really since their path type is used
        Files.createFile(ROOT_TEST_DIR.resolve("myFile.txt"));
        Files.createDirectory(ROOT_TEST_DIR.resolve("myDir2"));
        Files.createFile(ROOT_TEST_DIR.resolve("myDir2/myInnerFile.txt"));
        Files.createFile(ROOT_TEST_DIR.resolve("myDir2/nyOtherInnerFile.txt"));
        Files.createFile(ROOT_TEST_DIR.resolve("myDir2/myFutureDeletedFile.txt"));

        objectStore1.onCreateFile("myFile1.txt", "someHashOfFile1");
        objectStore1.onCreateFile("myFile2WhichIsDeletedOnTheOtherClient.txt", "someHashOfFile1");
        objectStore1.onCreateFile("myDir2", "someDirHash");
        objectStore1.onCreateFile("myDir2/myFutureDeletedFile.txt", "futureDeletedHash");
        objectStore1.onRemoveFile("myDir2/myFutureDeletedFile.txt");

        objectStore2.onCreateFile("myFile2WhichIsDeletedOnTheOtherClient.txt", "someHashOfFile1");
        objectStore2.onRemoveFile("myFile2WhichIsDeletedOnTheOtherClient.txt");
        objectStore2.onCreateFile("myDir2", "someDirHash");
        objectStore2.onModifyFile("myDir2", "someOtherHash"); // modify hash
        objectStore2.onCreateFile("myDir2/myInnerFile.txt", "hashOfInnerFile");
        objectStore2.onCreateFile("myDir2/myOtherInnerFile.txt", "hashOfInnerFile2");
        objectStore1.onCreateFile("myDir2/myFutureDeletedFile.txt", "futureDeletedHash"); // we remove this file if he has it but we don't

        HashMap<ObjectStore.MergedObjectType, Set<String>> outdatedOrMissingPaths = objectStore1.mergeObjectStore(objectStore2);
        Set<String> outDatedPaths = outdatedOrMissingPaths.get(ObjectStore.MergedObjectType.CHANGED);
        Set<String> deletedPaths = outdatedOrMissingPaths.get(ObjectStore.MergedObjectType.DELETED);

        assertThat("List should contain entry for myFile2WhichIsDeletedOnTheOtherClient.txt", deletedPaths, hasItem("myFile2WhichIsDeletedOnTheOtherClient.txt"));

        assertThat("List should contain entry for myDir2", outDatedPaths, hasItem("myDir2"));
        assertThat("List should contain entry for myDir2/myInnerFile.txt", outDatedPaths, hasItem("myDir2/myInnerFile.txt"));
        assertThat("List should contain entry for myDir2/myOtherInnerFile.txt", outDatedPaths, hasItem("myDir2/myOtherInnerFile.txt"));
    }
}
