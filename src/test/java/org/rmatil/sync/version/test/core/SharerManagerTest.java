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
import org.rmatil.sync.version.api.AccessType;
import org.rmatil.sync.version.api.IObjectManager;
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
import java.util.*;

import static org.junit.Assert.*;

public class SharerManagerTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    public static final Path ROOT_TEST_DIR = Config.DEFAULT.getRootTestDir();

    protected static IStorageAdapter storageAdapter;

    protected static ObjectManager objectManager;

    protected static SharerManager sharerManager;

    protected static PathObject pathObject;

    protected static Sharer sharer1;
    protected static Sharer sharer2;

    protected static String owner;

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

        owner = "Monsieur F";

        sharer1 = new Sharer("Eleanor Fant", AccessType.READ, new ArrayList<>());
        sharer2 = new Sharer("Eric Widget", AccessType.WRITE, new ArrayList<>());

        Set<Sharer> sharers = new HashSet<>();
        sharers.add(sharer1);
        sharers.add(sharer2);

        List<Version> versions = new ArrayList<>();

        pathObject = new PathObject("myFile.txt", "somePath/to/dir", PathType.FILE, AccessType.WRITE, true, true, owner, sharers, versions);

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

        sharerManager.addSharer("Piff Jenkins", AccessType.READ, pathObject.getAbsolutePath());

        List<String> sharingHistory = new ArrayList<>();
        sharingHistory.add(Hash.hash(org.rmatil.sync.version.config.Config.DEFAULT.getHashingAlgorithm(), AccessType.READ.name()));
        Sharer expectedSharer = new Sharer("Piff Jenkins", AccessType.READ, sharingHistory);

        Set<Sharer> sharers1 = sharerManager.getSharer(pathObject.getAbsolutePath());
        assertEquals("sharer do not contain s1", 3, sharers1.size());

        Iterator<Sharer> itr = sharers1.iterator();
        Sharer actualSharer = null;
        while (itr.hasNext()) {
            actualSharer = itr.next();
        }

        assertTrue("last sharer should be equal", expectedSharer.equals(actualSharer));

        IObjectManager objectManager = sharerManager.getObjectManager();
        PathObject fetchedObject = objectManager.getObjectForPath(pathObject.getAbsolutePath());

        assertTrue("File should be shared", fetchedObject.isShared());
        Set<Sharer> sharers2 = sharerManager.getSharer(pathObject.getAbsolutePath());
        assertEquals("Sharers2 should only contain three sharer", 3, sharers2.size());

        Iterator<Sharer> itr2 = sharerManager.getSharer(pathObject.getAbsolutePath()).iterator();
        Sharer actualSharer2 = null;
        while (itr2.hasNext()) {
            actualSharer2 = itr2.next();
        }

        assertEquals("Sharing history should contain only one value", 1, actualSharer2.getSharingHistory().size());

        // this sets the shared flag to false, if s1 is the last sharer of the file
        // and adds to the sharer's history one entry more and sets the access type to ACCESS_REMOVED
        sharerManager.removeSharer(expectedSharer.getUsername(), pathObject.getAbsolutePath());

        PathObject updatePathObject = sharerManager.getObjectManager().getObjectForPath(pathObject.getAbsolutePath());

        assertEquals("Only one sharer should be present", 3, updatePathObject.getSharers().size());

        Iterator<Sharer> itr3 = updatePathObject.getSharers().iterator();
        Sharer actualSharer3 = null;
        while (itr3.hasNext()) {
            actualSharer3 = itr3.next();
        }

        assertTrue("File should still be shared", updatePathObject.isShared());
        assertEquals("Sharer should be still present", 3, updatePathObject.getSharers().size());
        assertEquals("Sharer's access type should be removed", AccessType.ACCESS_REMOVED, actualSharer3.getAccessType());

        sharerManager.addOwner("owner", pathObject.getAbsolutePath());

        PathObject updatePathObject2 = sharerManager.getObjectManager().getObjectForPath(pathObject.getAbsolutePath());
        assertEquals("Owner should be equal", "owner", updatePathObject2.getOwner());
        assertEquals("Owner should be equal2", "owner", sharerManager.getOwner(pathObject.getAbsolutePath()));

        sharerManager.removeOwner(pathObject.getAbsolutePath());
        assertNull("Owner should be null after removal", sharerManager.getOwner(pathObject.getAbsolutePath()));


        sharerManager.removeSharer(sharer2.getUsername(), pathObject.getAbsolutePath());
        sharerManager.removeSharer(sharer1.getUsername(), pathObject.getAbsolutePath());

        assertTrue("File should not be shared anymore", sharerManager.getObjectManager().getObjectForPath(pathObject.getAbsolutePath()).isShared());
    }

    @Test
    public void testAccessor() {
        assertEquals("ObjectManager is not the same", objectManager, sharerManager.getObjectManager());
    }
}
