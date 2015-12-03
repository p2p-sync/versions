package org.rmatil.sync.version.core;

import org.rmatil.sync.commons.hashing.Hash;
import org.rmatil.sync.persistence.api.IPathElement;
import org.rmatil.sync.persistence.api.IStorageAdapter;
import org.rmatil.sync.persistence.api.StorageType;
import org.rmatil.sync.persistence.core.local.PathElement;
import org.rmatil.sync.persistence.exceptions.InputOutputException;
import org.rmatil.sync.version.api.IObjectManager;
import org.rmatil.sync.version.config.Config;
import org.rmatil.sync.version.core.model.PathObject;
import org.rmatil.sync.version.core.model.Index;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        IPathElement indexPath = new PathElement(this.indexFileName);

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

            this.index = new Index(new HashMap<>());
            this.storageAdapter.persist(StorageType.FILE, indexPath, this.index.toJson().getBytes());
        }
    }

    public void writeObject(PathObject path)
            throws InputOutputException {
        logger.trace("Writing path object for file " + path.getAbsolutePath());
        String fileNameHash = Hash.hash(Config.DEFAULT.getHashingAlgorithm(), path.getAbsolutePath());
        this.index.addPath(path.getAbsolutePath(), fileNameHash);

        logger.trace("Calculated hash for file name: " + fileNameHash);

        String pathToObject = this.createObjectDirIfNotExists(fileNameHash);

        IPathElement indexPath = new PathElement(this.indexFileName);
        IPathElement objectPath = new PathElement(pathToObject + "/" + fileNameHash + ".json");

        logger.trace("Writing path object to " + objectPath.getPath());
        logger.trace("Writing index to "  + indexPath.getPath());

        this.storageAdapter.persist(StorageType.FILE, objectPath, path.toJson().getBytes());
        this.storageAdapter.persist(StorageType.FILE, indexPath, this.index.toJson().getBytes());
    }

    public PathObject getObject(String fileNameHash)
            throws InputOutputException {
        String pathToHash = this.getAbsolutePathToHash(fileNameHash);

        IPathElement objectPath = new PathElement(pathToHash);

        byte[] content = this.storageAdapter.read(objectPath);
        String json = new String(content, StandardCharsets.UTF_8);

        return PathObject.fromJson(json);
    }

    public void removeObject(String fileNameHash)
            throws InputOutputException {
        String pathToHash = this.getAbsolutePathToHash(fileNameHash);

        PathObject pathObjectToDelete = this.getObject(fileNameHash);
        logger.trace("Removing path object for file " + pathObjectToDelete.getAbsolutePath());
        IPathElement objectPath = new PathElement(pathToHash);
        IPathElement indexPath = new PathElement(this.indexFileName);

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

    public List<PathObject> getChildren(String relativeParentFileName)
            throws InputOutputException {
        List<PathObject> children = new ArrayList<>();

        for (Map.Entry<String, String> entry : this.index.getPaths().entrySet()) {
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

    protected String createObjectDirIfNotExists(String hash)
            throws InputOutputException {
        String prefix = hash.substring(0, 2);
        String postfix = hash.substring(2);

        IPathElement prefixDir = new PathElement(prefix);
        IPathElement postfixDir = new PathElement(prefix + "/" + postfix);

        if (! this.storageAdapter.exists(StorageType.DIRECTORY, prefixDir)) {
            this.storageAdapter.persist(StorageType.DIRECTORY, prefixDir, null);
        }

        if (! this.storageAdapter.exists(StorageType.DIRECTORY, postfixDir)) {
            this.storageAdapter.persist(StorageType.DIRECTORY, postfixDir, null);
        }

        return prefix + "/" + postfix;
    }

    protected String getPathToHash(String hash) {
        String prefix = hash.substring(0, 2);
        String postfix = hash.substring(2);

        return prefix + "/" + postfix;
    }

    protected String getAbsolutePathToHash(String hash) {
        return this.getPathToHash(hash) + "/" + hash + ".json";
    }
}