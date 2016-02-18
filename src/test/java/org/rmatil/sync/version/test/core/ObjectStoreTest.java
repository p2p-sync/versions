package org.rmatil.sync.version.test.core;

import org.hamcrest.CoreMatchers;
import org.hamcrest.collection.IsEmptyCollection;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.rmatil.sync.commons.hashing.Hash;
import org.rmatil.sync.persistence.api.IStorageAdapter;
import org.rmatil.sync.persistence.core.local.LocalStorageAdapter;
import org.rmatil.sync.persistence.exceptions.InputOutputException;
import org.rmatil.sync.version.api.AccessType;
import org.rmatil.sync.version.api.PathType;
import org.rmatil.sync.version.core.ObjectStore;
import org.rmatil.sync.version.core.model.Index;
import org.rmatil.sync.version.core.model.PathObject;
import org.rmatil.sync.version.core.model.Sharer;
import org.rmatil.sync.version.core.model.Version;
import org.rmatil.sync.version.test.config.Config;
import org.rmatil.sync.version.test.util.APathTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNot.not;
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

        assertTrue("Index does not contain file", objectStore1.getObjectManager().getIndex().getPaths().containsKey(ROOT_TEST_DIR.relativize(testFile).toString()));
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

        assertTrue("Index does not contain file", objectStore1.getObjectManager().getIndex().getPaths().containsKey(ROOT_TEST_DIR.relativize(testDir).toString()));
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

        assertTrue(objectStore1.getObjectManager().getIndex().getPaths().containsKey(Paths.get("otherDir").resolve(testDir.getFileName()).resolve("myOtherFile.txt").toString()));
        assertTrue(objectStore1.getObjectManager().getIndex().getPaths().containsKey(Paths.get("otherDir").resolve(testDir.getFileName()).toString()));
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

        assertTrue(index.getPaths().containsKey(key1));
        assertTrue(index.getPaths().containsKey(key2));
        assertTrue(index.getPaths().containsKey(key3));

        // should not throw an exception
        objectStore1.getObjectManager().getObject(Hash.hash(Config.DEFAULT.getHashingAlgorithm(), key1));
        PathObject fileObject = objectStore1.getObjectManager().getObject(Hash.hash(Config.DEFAULT.getHashingAlgorithm(), key2));
        objectStore1.getObjectManager().getObject(Hash.hash(Config.DEFAULT.getHashingAlgorithm(), key3));

        Path writtenFile = Files.write(testDir.resolve("myOtherFile.txt"), "this is a modified string...".getBytes(), StandardOpenOption.APPEND);
        objectStore1.syncFile(writtenFile.toFile());
        PathObject fileObjectAfterSync = objectStore1.getObjectManager().getObject(Hash.hash(Config.DEFAULT.getHashingAlgorithm(), key2));

        assertEquals("Version size should be one", 1, fileObjectAfterSync.getVersions().size());
        assertNotEquals("File hash should be different", fileObject.getVersions().get(fileObject.getVersions().size() - 1).getHash(), fileObjectAfterSync.getVersions().get(fileObjectAfterSync.getVersions().size() - 1).getHash());

        // add an owner, a sharer to a file and change its hash and resync
        PathObject o1 = objectStore1.getObjectManager().getObjectForPath(key1);
        o1.setOwner("someOwner");
        Set<Sharer> sharers = new HashSet<>();
        sharers.add(new Sharer(
                "sharer",
                AccessType.WRITE,
                new ArrayList<>()
        ));
        o1.setSharers(sharers);
        o1.setAccessType(AccessType.READ);
        o1.setIsShared(true);

        objectStore1.getObjectManager().writeObject(o1);

        // now delete one file and resync
        Files.delete(testDir.resolve("myOtherFile.txt"));

        objectStore1.sync(ROOT_TEST_DIR.toFile());

        // check that owner, sharer, access type and isShared is still present
        PathObject pathObject1 = objectStore1.getObjectManager().getObjectForPath(key1);
        assertEquals("Owner should still be equal after change", "someOwner", pathObject1.getOwner());
        assertEquals("Sharer should still be contained", 1, pathObject1.getSharers().size());
        assertEquals("AccessType should still be read", AccessType.READ, pathObject1.getAccessType());
        assertTrue("File should still be shared", pathObject1.isShared());

        // check that delete file is still there
        assertNotNull("Deleted file should still exist in index", objectStore1.getObjectManager().getIndex().getPaths().get(key2));
        assertTrue("File should be flagged as deleted", objectStore1.getObjectManager().getObjectForPath(key2).isDeleted());


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
        Files.createFile(ROOT_TEST_DIR.resolve("someFileForMergingOfSharersAndOwners.txt"));
        Files.createDirectory(ROOT_TEST_DIR.resolve("myDirNotCausingConflicts"));

        objectStore1.onCreateFile("myFile1.txt", "someHashOfFile1");
        objectStore1.onCreateFile("myFile2WhichIsDeletedOnTheOtherClient.txt", "someHashOfFile1");
        objectStore1.onCreateFile("myDir2", "someDirHash");
        objectStore1.onCreateFile("myDir2/myFutureDeletedFile.txt", "futureDeletedHash");
        objectStore1.onRemoveFile("myDir2/myFutureDeletedFile.txt");
        objectStore1.onCreateFile("someFileForMergingOfSharersAndOwners.txt", "someHash");
        objectStore1.onCreateFile("myDirNotCausingConflicts", "someDirHash");

        objectStore2.onCreateFile("myFile2WhichIsDeletedOnTheOtherClient.txt", "someHashOfFile1");
        objectStore2.onRemoveFile("myFile2WhichIsDeletedOnTheOtherClient.txt");
        objectStore2.onCreateFile("myDir2", "someDirHash");
        objectStore2.onModifyFile("myDir2", "someOtherHash"); // modify hash
        objectStore2.onCreateFile("myDir2/myInnerFile.txt", "hashOfInnerFile");
        objectStore2.onCreateFile("myDir2/myOtherInnerFile.txt", "hashOfInnerFile2");
        objectStore2.onCreateFile("myDir2/myFutureDeletedFile.txt", "futureDeletedHash"); // we remove this file if he has it but we don't
        objectStore2.onCreateFile("someFileForMergingOfSharersAndOwners.txt", "someHash");
        objectStore2.onCreateFile("myDirNotCausingConflicts", "someDifferentDirHash");

        // now write different sharers and an empty owner
        List<String> sharingHistory11 = new ArrayList<>();
        sharingHistory11.add("hash1");

        Sharer sharer11 = new Sharer(
                "sharer1",
                AccessType.WRITE,
                sharingHistory11
        );

        PathObject o1 = objectStore1.getObjectManager().getObjectForPath("someFileForMergingOfSharersAndOwners.txt");
        o1.getSharers().add(sharer11);
        objectStore1.getObjectManager().writeObject(o1);

        List<String> sharingHistory21 = new ArrayList<>();
        sharingHistory21.add("hash1");
        sharingHistory21.add("hash2");
        Sharer sharer21 = new Sharer(
                "sharer1",
                AccessType.WRITE,
                sharingHistory21
        );

        List<String> sharingHistory22 = new ArrayList<>();
        sharingHistory22.add("hash1");
        Sharer sharer22 = new Sharer(
                "sharer2",
                AccessType.WRITE,
                sharingHistory22
        );

        PathObject o2 = objectStore2.getObjectManager().getObjectForPath("someFileForMergingOfSharersAndOwners.txt");
        o2.getSharers().add(sharer21);
        o2.getSharers().add(sharer22);
        o2.setOwner("someOwner");
        objectStore2.getObjectManager().writeObject(o2);

        HashMap<ObjectStore.MergedObjectType, Set<String>> outdatedOrMissingPaths = objectStore1.mergeObjectStore(objectStore2);
        Set<String> outDatedPaths = outdatedOrMissingPaths.get(ObjectStore.MergedObjectType.CHANGED);
        Set<String> deletedPaths = outdatedOrMissingPaths.get(ObjectStore.MergedObjectType.DELETED);
        Set<String> conflictPaths = outdatedOrMissingPaths.get(ObjectStore.MergedObjectType.CONFLICT);

        assertThat("There should be no conflict file", conflictPaths, is(IsEmptyCollection.empty()));

        assertThat("List should contain entry for myFile2WhichIsDeletedOnTheOtherClient.txt", deletedPaths, hasItem("myFile2WhichIsDeletedOnTheOtherClient.txt"));

        assertThat("List should contain entry for myDir2", outDatedPaths, hasItem("myDir2"));
        assertThat("List should contain entry for myDir2/myInnerFile.txt", outDatedPaths, hasItem("myDir2/myInnerFile.txt"));
        assertThat("List should contain entry for myDir2/myOtherInnerFile.txt", outDatedPaths, hasItem("myDir2/myOtherInnerFile.txt"));


        PathObject o3 = objectStore1.getObjectManager().getObjectForPath("someFileForMergingOfSharersAndOwners.txt");
        assertEquals("There should now exist two sharers", 2, o3.getSharers().size());
        assertEquals("Sharing history of sharer1 should contain 2 entries", 2, o3.getSharers().iterator().next().getSharingHistory().size());
        assertEquals("Owner should be set now", "someOwner", o3.getOwner());

        objectStore1.getObjectManager().clear();
        objectStore2.getObjectManager().clear();

        // this should be a conflict
        objectStore1.onCreateFile("myFile.txt", "someHash");
        objectStore2.onCreateFile("myFile.txt", "someOtherHash");

        outdatedOrMissingPaths = objectStore1.mergeObjectStore(objectStore2);
        outDatedPaths = outdatedOrMissingPaths.get(ObjectStore.MergedObjectType.CHANGED);
        deletedPaths = outdatedOrMissingPaths.get(ObjectStore.MergedObjectType.DELETED);
        conflictPaths = outdatedOrMissingPaths.get(ObjectStore.MergedObjectType.CONFLICT);

        assertThat("There should be no outdated sharedPaths", outDatedPaths, is(IsEmptyCollection.empty()));
        assertThat("There should be no deleted sharedPaths", deletedPaths, is(IsEmptyCollection.empty()));
        assertThat("There should be conflict sharedPaths", conflictPaths, is(not(IsEmptyCollection.empty())));

        assertThat("There is the conflict path", conflictPaths, hasItem("myFile.txt"));
        assertThat("There should be no conflict item for conflicting directory", conflictPaths, CoreMatchers.not(hasItem("myDirNotCausingConflicts")));


        objectStore1.getObjectManager().clear();
        objectStore2.getObjectManager().clear();

        objectStore1.onCreateFile("myFile.txt", "someHash");
        objectStore2.onCreateFile("myFile.txt", "someHash");
        // this should be a conflict
        objectStore1.onModifyFile("myFile.txt", "someConflictHash");
        objectStore2.onModifyFile("myFile.txt", "someOtherConflictHash");

        outdatedOrMissingPaths = objectStore1.mergeObjectStore(objectStore2);
        outDatedPaths = outdatedOrMissingPaths.get(ObjectStore.MergedObjectType.CHANGED);
        deletedPaths = outdatedOrMissingPaths.get(ObjectStore.MergedObjectType.DELETED);
        conflictPaths = outdatedOrMissingPaths.get(ObjectStore.MergedObjectType.CONFLICT);

        assertThat("There should be no outdated sharedPaths", outDatedPaths, is(IsEmptyCollection.empty()));
        assertThat("There should be no deleted sharedPaths", deletedPaths, is(IsEmptyCollection.empty()));
        assertThat("There should be conflict sharedPaths", conflictPaths, is(not(IsEmptyCollection.empty())));

        assertThat("There is the conflict path", conflictPaths, hasItem("myFile.txt"));

        objectStore1.getObjectManager().clear();
        objectStore2.getObjectManager().clear();

        // check for all versions to be contained
        objectStore2.onCreateFile("myFile.txt", "someHash");
        objectStore2.onModifyFile("myFile.txt", "someConflictHash");

        outdatedOrMissingPaths = objectStore1.mergeObjectStore(objectStore2);
        outDatedPaths = outdatedOrMissingPaths.get(ObjectStore.MergedObjectType.CHANGED);
        deletedPaths = outdatedOrMissingPaths.get(ObjectStore.MergedObjectType.DELETED);
        conflictPaths = outdatedOrMissingPaths.get(ObjectStore.MergedObjectType.CONFLICT);

        assertThat("There should be outdated sharedPaths", outDatedPaths, is(not(IsEmptyCollection.empty())));
        assertThat("There should be no deleted sharedPaths", deletedPaths, is(IsEmptyCollection.empty()));
        assertThat("There should be no conflict sharedPaths", conflictPaths, is(IsEmptyCollection.empty()));

        assertThat("There is the outdated path", outDatedPaths, hasItem("myFile.txt"));

        PathObject pathObject = objectStore1.getObjectManager().getObjectForPath("myFile.txt");
        assertThat("Versions must contain both hashes", pathObject.getVersions(), hasItems(new Version("someHash"), new Version("someConflictHash")));
        assertEquals("Last version must be someConflictHash", pathObject.getVersions().get(pathObject.getVersions().size() - 1).getHash(), "someConflictHash");

        objectStore1.getObjectManager().clear();
        objectStore2.getObjectManager().clear();

        // this should be a conflict
        objectStore1.onCreateFile("myFile.txt", "someHash");
        objectStore2.onCreateFile("myFile.txt", "someOtherHash");

        outdatedOrMissingPaths = objectStore1.mergeObjectStore(objectStore2);
        outDatedPaths = outdatedOrMissingPaths.get(ObjectStore.MergedObjectType.CHANGED);
        deletedPaths = outdatedOrMissingPaths.get(ObjectStore.MergedObjectType.DELETED);
        conflictPaths = outdatedOrMissingPaths.get(ObjectStore.MergedObjectType.CONFLICT);

        assertThat("There should be no outdated sharedPaths", outDatedPaths, is(IsEmptyCollection.empty()));
        assertThat("There should be no deleted sharedPaths", deletedPaths, is(IsEmptyCollection.empty()));
        assertThat("There should be conflict sharedPaths", conflictPaths, is(not(IsEmptyCollection.empty())));

        assertThat("There is the conflict path", conflictPaths, hasItem("myFile.txt"));
    }

    @Test
    public void accessorTests() {
        assertNotNull("sharer manager should be instantiated", objectStore1.getSharerManager());
        assertNotNull("sharer manager should be instantiated", objectStore2.getSharerManager());
    }
}
