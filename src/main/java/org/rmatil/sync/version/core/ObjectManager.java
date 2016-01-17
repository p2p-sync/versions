package org.rmatil.sync.version.core;

import org.rmatil.sync.commons.hashing.Hash;
import org.rmatil.sync.persistence.api.IPathElement;
import org.rmatil.sync.persistence.api.IStorageAdapter;
import org.rmatil.sync.persistence.api.StorageType;
import org.rmatil.sync.persistence.core.local.LocalPathElement;
import org.rmatil.sync.persistence.exceptions.InputOutputException;
import org.rmatil.sync.version.api.IObjectManager;
import org.rmatil.sync.version.config.Config;
import org.rmatil.sync.version.core.model.PathObject;
import org.rmatil.sync.version.core.model.Index;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class ObjectManager implements IObjectManager {

    private static final Logger logger = LoggerFactory.getLogger(ObjectManager.class);

    protected IStorageAdapter storageAdapter;

    protected String indexFileName;

    protected String objectDirName;

    protected Index index;

    public ObjectManager(String indexFileName, String objectDirName, IStorageAdapter storageAdapter)
            throws InputOutputException {
        this.storageAdapter = storageAdapter;
        this.indexFileName = indexFileName;
        this.objectDirName = objectDirName;

        IPathElement indexPath = new LocalPathElement(this.indexFileName);

        try {
            // create the index from the stored file
            logger.trace("Trying to read from existing index file");

            byte[] content = this.storageAdapter.read(indexPath);
            String json = new String(content, StandardCharsets.UTF_8);
            this.index = Index.fromJson(json);
        } catch (InputOutputException e) {
            // the file does not exist yet, so we have to create it
            logger.error(e.getMessage());
            logger.info("Creating the index file at " + this.indexFileName);

            this.index = new Index(new HashMap<>(), new HashMap<>());
            this.storageAdapter.persist(StorageType.FILE, indexPath, this.index.toJson().getBytes());
        }
    }

    public synchronized void clear()
            throws InputOutputException {
        LocalPathElement objectPath = new LocalPathElement(this.objectDirName);
        LocalPathElement indexPath = new LocalPathElement(this.indexFileName);

        // delete all objects
        if (this.storageAdapter.exists(StorageType.DIRECTORY, objectPath)) {
            this.storageAdapter.delete(objectPath);
        } else {
            logger.info("Could not remove object folder (No such file or directory)");
        }

        // recreate empty index
        this.index = new Index(new HashMap<>(), new HashMap<>());

        this.storageAdapter.persist(StorageType.FILE, indexPath, this.index.toJson().getBytes());
    }

    public synchronized void writeObject(PathObject path)
            throws InputOutputException {
        logger.trace("Writing path object for file " + path.getAbsolutePath());
        String fileNameHash = Hash.hash(Config.DEFAULT.getHashingAlgorithm(), path.getAbsolutePath());
        this.index.addPath(path.getAbsolutePath(), fileNameHash);

        // add file id to path object
        UUID fileId = this.index.getPathIdentifiers().get(path.getAbsolutePath());
        path.setFileId(fileId);

        logger.trace("Calculated hash for file name: " + fileNameHash);

        String pathToObject = this.createObjectDirIfNotExists(fileNameHash);

        IPathElement indexPath = new LocalPathElement(this.indexFileName);
        IPathElement objectPath = new LocalPathElement(pathToObject + "/" + fileNameHash + ".json");

        logger.trace("Writing path object to " + objectPath.getPath());
        logger.trace("Writing index to " + indexPath.getPath());

        this.storageAdapter.persist(StorageType.FILE, objectPath, path.toJson().getBytes());
        this.storageAdapter.persist(StorageType.FILE, indexPath, this.index.toJson().getBytes());
    }

    public synchronized PathObject getObject(String fileNameHash)
            throws InputOutputException {
        String pathToHash = this.getAbsolutePathToHash(fileNameHash);

        IPathElement objectPath = new LocalPathElement(pathToHash);

        byte[] content = this.storageAdapter.read(objectPath);
        String json = new String(content, StandardCharsets.UTF_8);

        return PathObject.fromJson(json);
    }

    public synchronized PathObject getObjectForPath(String relativeFilePath)
            throws InputOutputException {
        UUID fileId = this.getIndex().getPathIdentifiers().get(relativeFilePath);

        if (null == fileId) {
            throw new InputOutputException("Could not read object for path " + relativeFilePath + ". No such file or directory");
        }

        String hashToFile = this.getIndex().getPaths().get(fileId);

        return this.getObject(hashToFile);
    }

    public synchronized void removeObject(String fileNameHash)
            throws InputOutputException {
        String pathToHash = this.getAbsolutePathToHash(fileNameHash);

        PathObject pathObjectToDelete = this.getObject(fileNameHash);
        logger.trace("Removing path object for file " + pathObjectToDelete.getAbsolutePath());
        IPathElement objectPath = new LocalPathElement(pathToHash);
        IPathElement indexPath = new LocalPathElement(this.indexFileName);

        // remove object file, i.e. the file containing versions, ...
        if (this.storageAdapter.exists(StorageType.FILE, objectPath)) {
            logger.trace("Removing old path object " + objectPath.getPath());
            this.storageAdapter.delete(objectPath);
        }

        logger.trace("Removing file from index...");
        this.index.removePath(pathObjectToDelete.getAbsolutePath());
        this.storageAdapter.persist(StorageType.FILE, indexPath, this.index.toJson().getBytes());
        logger.trace("Rewriting index after removing of file " + pathObjectToDelete.getAbsolutePath());
    }

    public synchronized List<PathObject> getChildren(String relativeParentFileName)
            throws InputOutputException {
        List<PathObject> children = new ArrayList<>();

        for (Map.Entry<String, UUID> entry : this.index.getPathIdentifiers().entrySet()) {
            // the parent is logically a directory, so to avoid getting the parent directory too,
            // we can add a slash on the path to the parent dir
            if (entry.getKey().startsWith(relativeParentFileName + "/")) {
                children.add(this.getObject(Hash.hash(Config.DEFAULT.getHashingAlgorithm(), entry.getKey())));
            }
        }

        return children;
    }

    public Index getIndex() {
        return this.index;
    }

    public String getIndexFileName() {
        return this.indexFileName;
    }

    @Override
    public IPathElement getObjectDir() {
        return new LocalPathElement(this.objectDirName);
    }

    @Override
    public IStorageAdapter getStorageAdapater() {
        return this.storageAdapter;
    }

    protected synchronized String createObjectDirIfNotExists(String hash)
            throws InputOutputException {
        String prefix = hash.substring(0, 2);
        String postfix = hash.substring(2);

        IPathElement objectDir = new LocalPathElement(this.objectDirName);
        if (! this.storageAdapter.exists(StorageType.DIRECTORY, objectDir)) {
            this.storageAdapter.persist(StorageType.DIRECTORY, objectDir, null);
        }

        IPathElement prefixDir = new LocalPathElement(this.objectDirName + "/" + prefix);
        IPathElement postfixDir = new LocalPathElement(this.objectDirName + "/" + prefix + "/" + postfix);

        if (! this.storageAdapter.exists(StorageType.DIRECTORY, prefixDir)) {
            this.storageAdapter.persist(StorageType.DIRECTORY, prefixDir, null);
        }

        if (! this.storageAdapter.exists(StorageType.DIRECTORY, postfixDir)) {
            this.storageAdapter.persist(StorageType.DIRECTORY, postfixDir, null);
        }

        return this.objectDirName + "/" + prefix + "/" + postfix;
    }

    protected String getPathToHash(String hash) {
        String prefix = hash.substring(0, 2);
        String postfix = hash.substring(2);

        return this.objectDirName + "/" + prefix + "/" + postfix;
    }

    protected String getAbsolutePathToHash(String hash) {
        return this.getPathToHash(hash) + "/" + hash + ".json";
    }
}
