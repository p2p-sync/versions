package org.rmatil.sync.version.test.core;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.rmatil.sync.commons.hashing.Hash;
import org.rmatil.sync.persistence.api.IPathElement;
import org.rmatil.sync.persistence.api.IStorageAdapter;
import org.rmatil.sync.persistence.api.StorageType;
import org.rmatil.sync.persistence.core.local.LocalStorageAdapter;
import org.rmatil.sync.persistence.core.local.PathElement;
import org.rmatil.sync.persistence.exceptions.InputOutputException;
import org.rmatil.sync.version.api.AccessType;
import org.rmatil.sync.version.api.PathType;
import org.rmatil.sync.version.core.ObjectManager;
import org.rmatil.sync.version.core.model.PathObject;
import org.rmatil.sync.version.core.model.Sharer;
import org.rmatil.sync.version.core.model.Version;
import org.rmatil.sync.version.test.config.Config;
import org.rmatil.sync.version.test.util.FileUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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

        Sharer sharer1 = new Sharer("192.168.1.1", AccessType.READ);
        Sharer sharer2 = new Sharer("192.168.3.2", AccessType.WRITE);

        List<Sharer> sharers = new ArrayList<Sharer>();
        sharers.add(sharer1);
        sharers.add(sharer2);

        Version v1 = new Version("hashOfContent");
        Version v2 = new Version("hashOfContentAfterModifying");

        List<Version> versions = new ArrayList<Version>();
        versions.add(v1);
        versions.add(v2);

        pathObject = new PathObject("myFile.txt", "somePath/to/dir", PathType.FILE, true, sharers, versions);
    }

    @AfterClass
    public static void tearDown() {
        FileUtil.delete(ROOT_TEST_DIR.toFile());
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
        assertEquals("Index does not contain file hash at file dir", fileNameHash, objectManager.getIndex().getPaths().get(pathObject.getAbsolutePath()));

        // check object
        String prefix = fileNameHash.substring(0, 2);
        String postifx = fileNameHash.substring(2);

        IPathElement pathToObject = new PathElement(prefix + "/" + postifx + "/" + fileNameHash + ".json");

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

        assertFalse("Objects should not be equal", pathObject.equals(readObject));
        assertEquals("Object content should be equal", pathObject.toJson(), readObject.toJson());
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

}
