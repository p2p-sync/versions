package org.rmatil.sync.version.test.core;

import org.junit.*;
import org.junit.rules.ExpectedException;
import org.rmatil.sync.commons.hashing.Hash;
import org.rmatil.sync.persistence.api.IPathElement;
import org.rmatil.sync.persistence.api.IStorageAdapter;
import org.rmatil.sync.persistence.api.StorageType;
import org.rmatil.sync.persistence.core.local.LocalPathElement;
import org.rmatil.sync.persistence.core.local.LocalStorageAdapter;
import org.rmatil.sync.persistence.exceptions.InputOutputException;
import org.rmatil.sync.version.api.AccessType;
import org.rmatil.sync.version.api.DeleteType;
import org.rmatil.sync.version.api.PathType;
import org.rmatil.sync.version.core.ObjectManager;
import org.rmatil.sync.version.core.SharerManager;
import org.rmatil.sync.version.core.model.*;
import org.rmatil.sync.version.test.config.Config;
import org.rmatil.sync.version.test.util.FileUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class ObjectManagerTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    public static final Path ROOT_TEST_DIR = Config.DEFAULT.getRootTestDir();

    protected static IStorageAdapter storageAdapter;

    protected static ObjectManager objectManager;

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

        String owner = "Valentino Morose";

        Sharer sharer1 = new Sharer("Natalya Undergrowth", AccessType.READ, new ArrayList<>());
        Sharer sharer2 = new Sharer("Archibald Northbottom", AccessType.WRITE, new ArrayList<>());

        Set<Sharer> sharers = new HashSet<>();
        sharers.add(sharer1);
        sharers.add(sharer2);

        Version v1 = new Version("hashOfContent");
        Version v2 = new Version("hashOfContentAfterModifying");

        List<Version> versions = new ArrayList<>();
        versions.add(v1);
        versions.add(v2);

        pathObject = new PathObject("myFile.txt", "somePath/to/dir", PathType.FILE, AccessType.WRITE, true, new Delete(null, new ArrayList<>()), owner, sharers, versions);
    }

    @AfterClass
    public static void tearDown() {
        FileUtil.delete(ROOT_TEST_DIR.toFile());
    }

    @Before
    public void before()
            throws InputOutputException {
        objectManager.clear();
    }

    @Test
    public void testVersionManager()
            throws IOException, InputOutputException {
        // test constructor
        String expectedJson = "{\n" +
                "  \"paths\": {}\n" +
                "}";

        byte[] content = Files.readAllBytes(ROOT_TEST_DIR.resolve(objectManager.getIndexFileName()));
        String json = new String(content, StandardCharsets.UTF_8);

        assertEquals("Json content is not the same", expectedJson, json);

        // wait a bit for the file creation
        try {
            Thread.sleep(1000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // now let's create another version manager for the same root test dir
        // which then should read the index from the created file again
        ObjectManager objectManager2 = new ObjectManager("index.json", "objects", new LocalStorageAdapter(ROOT_TEST_DIR));
    }

    @Test
    public void testVersionManagerException()
            throws InputOutputException {
        thrown.expect(InputOutputException.class);

        objectManager = new ObjectManager("someDir/otherDir/someIndex.json", "objects", new LocalStorageAdapter(ROOT_TEST_DIR));
    }

    @Test
    public void testWriteObject()
            throws InputOutputException {

        objectManager.writeObject(pathObject);

        String fileNameHash = Hash.hash(org.rmatil.sync.version.config.Config.DEFAULT.getHashingAlgorithm(), pathObject.getAbsolutePath());

        assertTrue("Index does not contain new file", objectManager.getIndex().getPaths().containsKey(pathObject.getAbsolutePath()));
        assertTrue("Index does not contain new file hash", objectManager.getIndex().getPaths().containsValue(fileNameHash));

        // check object
        String prefix = fileNameHash.substring(0, 2);
        String postifx = fileNameHash.substring(2);

        IPathElement pathToObject = new LocalPathElement("objects/" + prefix + "/" + postifx + "/" + fileNameHash + ".json");

        assertTrue("Object was not created", storageAdapter.exists(StorageType.FILE, pathToObject));

        byte[] content = storageAdapter.read(pathToObject);
        String json = new String(content, StandardCharsets.UTF_8);

        assertEquals("Json is not equal", pathObject.toJson(), json);
    }

    @Test
    public void testGetObject()
            throws InputOutputException {
        objectManager.writeObject(pathObject);

        String fileNameHash = Hash.hash(org.rmatil.sync.version.config.Config.DEFAULT.getHashingAlgorithm(), pathObject.getAbsolutePath());

        PathObject readObject = objectManager.getObject(fileNameHash);

        PathObject readObject2 = objectManager.getObjectForPath(pathObject.getAbsolutePath());

        assertFalse("Objects should not be equal", pathObject.equals(readObject));
        assertEquals("Object content should be equal", pathObject.toJson(), readObject.toJson());
        assertEquals("Object contents should be equal", readObject.toJson(), readObject2.toJson());
    }

    @Test
    public void testGetHashForPath() {
        String fileName = "sync/my/path/to/myFile.txt";

        assertEquals("Hash should be equal", Hash.hash(Config.DEFAULT.getHashingAlgorithm(), fileName), objectManager.getHashForPath(fileName));
    }

    @Test
    public void testRemoveObject()
            throws InputOutputException {
        thrown.expect(InputOutputException.class);

        objectManager.writeObject(pathObject);

        String fileNameHash = Hash.hash(org.rmatil.sync.version.config.Config.DEFAULT.getHashingAlgorithm(), pathObject.getAbsolutePath());

        PathObject readObject = objectManager.getObject(fileNameHash);

        assertFalse("Objects should not be equal", pathObject.equals(readObject));
        assertEquals("Object content should be equal", pathObject.toJson(), readObject.toJson());

        objectManager.removeObject(fileNameHash);

        // this should throw an exception, that the file does not exist
        objectManager.getObject(fileNameHash);
    }

    @Test
    public void testGetChildren()
            throws InputOutputException {
        objectManager.writeObject(pathObject);

        PathObject dirObject = new PathObject("dir", "somePath/to", PathType.DIRECTORY, AccessType.WRITE, false, new Delete(null, new ArrayList<>()), null, new HashSet<>(), new ArrayList<>());
        objectManager.writeObject(dirObject);

        List<PathObject> children = objectManager.getChildren("somePath/to/dir");

        assertFalse("Children contain a path", children.isEmpty());
        assertEquals("Children contain only one path", 1, children.size());
        assertEquals("PathObject is not equal", pathObject.toJson(), children.get(0).toJson());

        objectManager.removeObject(Hash.hash(Config.DEFAULT.getHashingAlgorithm(), dirObject.getAbsolutePath()));
        objectManager.removeObject(Hash.hash(Config.DEFAULT.getHashingAlgorithm(), pathObject.getAbsolutePath()));
    }

    @Test
    public void testClear()
            throws InputOutputException {
        objectManager.writeObject(pathObject);

        PathObject dirObject = new PathObject("dir", "somePath/to", PathType.DIRECTORY, AccessType.WRITE, false, new Delete(null, new ArrayList<>()), null, new HashSet<>(), new ArrayList<>());
        objectManager.writeObject(dirObject);

        PathObject anotherObject = new PathObject("anotherFile.txt", "somePath/to/dir", PathType.FILE, AccessType.WRITE, false, new Delete(null, new ArrayList<>()), null, new HashSet<>(), new ArrayList<>());
        objectManager.writeObject(anotherObject);

        Index origIndex = objectManager.getIndex();
        assertEquals("not all files are in the index", 3, origIndex.getPaths().size());

        String pathObjectHash = Hash.hash(Config.DEFAULT.getHashingAlgorithm(), pathObject.getAbsolutePath());
        assertNotNull("pathObject should not be null", objectManager.getObject(pathObjectHash));
        String dirObjectHash = Hash.hash(Config.DEFAULT.getHashingAlgorithm(), dirObject.getAbsolutePath());
        assertNotNull("pathObject should not be null", objectManager.getObject(dirObjectHash));
        String anotherObjectHash = Hash.hash(Config.DEFAULT.getHashingAlgorithm(), anotherObject.getAbsolutePath());
        assertNotNull("pathObject should not be null", objectManager.getObject(anotherObjectHash));

        objectManager.clear();

        assertEquals("Paths in index are not cleared", 0, objectManager.getIndex().getPaths().size());

        thrown.expect(InputOutputException.class);
        objectManager.getObject(pathObjectHash);

        thrown.expect(InputOutputException.class);
        objectManager.getObject(dirObjectHash);

        thrown.expect(InputOutputException.class);
        objectManager.getObject(anotherObjectHash);
    }

    @Test
    public void testShareObject()
            throws InputOutputException {
        PathObject sharedPathObject = new PathObject(
                pathObject.getName(),
                pathObject.getPath(),
                pathObject.getPathType(),
                AccessType.WRITE,
                false,
                new Delete(DeleteType.EXISTENT, new ArrayList<>()),
                null,
                new HashSet<>(),
                new ArrayList<>()
        );

        objectManager.writeObject(sharedPathObject);

        assertEquals("One path should exist", 1, objectManager.getIndex().getPaths().size());

        SharerManager sharerManager = new SharerManager(objectManager);

        PathObject fetchedObject = objectManager.getObjectForPath(sharedPathObject.getAbsolutePath());

        assertNotNull("Fetched path object should not be null", fetchedObject);

        // start to "share" the file
        sharerManager.addOwner("Valentino Morose", sharedPathObject.getAbsolutePath());

        sharerManager.addSharer(
                "Manuel Internetiquette",
                AccessType.WRITE,
                sharedPathObject.getAbsolutePath()
        );

        PathObject objectForFileId = objectManager.getObjectForPath(sharedPathObject.getAbsolutePath());

        assertNotNull("ObjectForFileId should not be null", objectForFileId);
        assertTrue("File should be shared", objectForFileId.isShared());
        assertEquals("There should be one sharer", 1, objectForFileId.getSharers().size());
        assertEquals("SharerName should be equal", "Manuel Internetiquette", objectForFileId.getSharers().iterator().next().getUsername());
        assertEquals("AccessType should be equal", AccessType.WRITE, objectForFileId.getSharers().iterator().next().getAccessType());
        assertEquals("Owner should be equal", "Valentino Morose", objectForFileId.getOwner());
        assertEquals("Path should be existent", DeleteType.EXISTENT, objectForFileId.getDeleted().getDeleteType());
        assertEquals("No delete history entry should exist", 0, objectForFileId.getDeleted().getDeleteHistory().size());
    }
}
