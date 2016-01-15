package org.rmatil.sync.version.core;

import org.rmatil.sync.commons.hashing.Hash;
import org.rmatil.sync.commons.path.Naming;
import org.rmatil.sync.persistence.api.IStorageAdapter;
import org.rmatil.sync.persistence.exceptions.InputOutputException;
import org.rmatil.sync.version.api.IObjectManager;
import org.rmatil.sync.version.api.IObjectStore;
import org.rmatil.sync.version.api.IVersionManager;
import org.rmatil.sync.version.api.PathType;
import org.rmatil.sync.version.config.Config;
import org.rmatil.sync.version.core.model.PathObject;
import org.rmatil.sync.version.core.model.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ObjectStore implements IObjectStore {

    final static Logger logger = LoggerFactory.getLogger(ObjectStore.class);

    protected Path rootDir;

    protected IStorageAdapter storageAdapter;

    protected IObjectManager objectManager;

    protected IVersionManager versionManager;

    public ObjectStore(Path rootDir, String indexFileName, String objectDirName, IStorageAdapter storageAdapter)
            throws InputOutputException {
        this.rootDir = rootDir;
        this.objectManager = new ObjectManager(indexFileName, objectDirName, storageAdapter);
        this.versionManager = new VersionManager(this.objectManager);
        this.storageAdapter = storageAdapter;
    }

    public void sync(File rootSyncDir)
            throws InputOutputException {
        if (null == rootSyncDir || ! rootSyncDir.exists()) {
            throw new InputOutputException("Can not sync index. Root of synchronized folder does not exist.");
        }

        // clear object store
        this.objectManager.clear();

        File[] files = rootSyncDir.listFiles();
        if (null == files) {
            logger.info("Abort sync of object store, no files in given directory");
            return;
        }

        // recreate objects
        for (File file : files) {
            Path relativeSyncFolder = this.rootDir.relativize(this.storageAdapter.getRootDir());
            if (file.getAbsolutePath().equals(this.rootDir.resolve(relativeSyncFolder).toFile().getAbsolutePath())) {
                // ignore sync folder
                logger.trace("Ignoring sync folder from being created in the index");
                continue;
            }

            this.syncChild(file);
        }
    }

    protected void syncChild(File file)
            throws InputOutputException {
        Path relativePathToRootDir = this.rootDir.relativize(file.toPath());

        try {
            // throws an exception if path does not exist
            PathObject oldObject = this.objectManager.getObject(Hash.hash(Config.DEFAULT.getHashingAlgorithm(), relativePathToRootDir.toString()));
        } catch (InputOutputException e) {
            // file does not exist yet, so we create it
            logger.debug("No object stored for file " + relativePathToRootDir.toString() + ". Creating...");
            try {

                String hash = null;
                if (file.isFile() || file.isDirectory()) {
                    hash = Hash.hash(Config.DEFAULT.getHashingAlgorithm(), file);
                }

                this.onCreateFile(relativePathToRootDir.toString(), hash);
            } catch (IOException e1) {
                logger.error("Could not create path object for file " + relativePathToRootDir.toString() + ". Message: " + e1.getMessage());
            }
        }

        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (null == children) {
                logger.trace("Children does not contain any children. Stopping to sync deeper");
                return;
            }

            for (File child : children) {
                this.syncChild(child);
            }
        }
    }

    public void onCreateFile(String relativePath, String contentHash)
            throws InputOutputException {
        logger.debug("Creating object for " + relativePath);

        Path relativePathToWatchedDir = Paths.get(relativePath);
        Path absolutePathOnFs = this.rootDir.resolve(relativePathToWatchedDir);

        // filename.txt
        // myPath/to/filename.txt
        int idx = relativePath.lastIndexOf(relativePathToWatchedDir.getFileName().toString());
        String pathToFileWithoutFilename = relativePath.substring(0, idx);

        if (pathToFileWithoutFilename.endsWith("/")) {
            pathToFileWithoutFilename = pathToFileWithoutFilename.substring(0, pathToFileWithoutFilename.length() - 1);
        }

        PathType pathType = null;
        if (absolutePathOnFs.toFile().isDirectory()) {
            pathType = PathType.DIRECTORY;
        } else if (absolutePathOnFs.toFile().isFile()) {
            pathType = PathType.FILE;
        }

        Version v1 = new Version(contentHash);
        List<Version> versions = new ArrayList<>();
        versions.add(v1);

        PathObject pathObject = new PathObject(
                relativePathToWatchedDir.getFileName().toString(),
                null, // is filled in by the object manager
                pathToFileWithoutFilename,
                pathType,
                false,
                false,
                new ArrayList<>(),
                versions
        );

        this.objectManager.writeObject(pathObject);
    }

    public void onModifyFile(String relativePath, String contentHash)
            throws InputOutputException {
        logger.debug("Modifying object for " + relativePath);
        this.versionManager.addVersion(new Version(contentHash), relativePath);
    }

    public void onRemoveFile(String relativePath)
            throws InputOutputException {
        logger.debug("Removing object for " + relativePath);

        // just setting the deleted flag

        PathObject object = this.objectManager.getObject(Hash.hash(Config.DEFAULT.getHashingAlgorithm(), relativePath));
        PathObject deletedObject = new PathObject(
                object.getName(),
                object.getFileId(),
                Naming.getPathWithoutFileName(object.getName(), relativePath),
                object.getPathType(),
                object.isShared(),
                true,
                object.getSharers(),
                object.getVersions()
        );

        this.objectManager.writeObject(deletedObject);
    }

    public void onMoveFile(String oldRelativePath, String newRelativePath)
            throws InputOutputException {
        logger.debug("Moving object for " + oldRelativePath);

        PathObject oldObject = this.objectManager.getObject(Hash.hash(Config.DEFAULT.getHashingAlgorithm(), oldRelativePath));
        PathObject newObject = new PathObject(
                oldObject.getName(),
                oldObject.getFileId(),
                Naming.getPathWithoutFileName(oldObject.getName(), newRelativePath),
                oldObject.getPathType(),
                oldObject.isShared(),
                oldObject.isDeleted(),
                oldObject.getSharers(),
                oldObject.getVersions()
        );

        this.objectManager.writeObject(newObject);
        this.objectManager.removeObject(Hash.hash(Config.DEFAULT.getHashingAlgorithm(), oldRelativePath));

    }

    public IObjectManager getObjectManager() {
        return this.objectManager;
    }
}
