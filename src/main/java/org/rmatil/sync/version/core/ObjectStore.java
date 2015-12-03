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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ObjectStore implements IObjectStore {

    final static Logger logger = LoggerFactory.getLogger(ObjectStore.class);

    protected Path rootDir;

    protected IObjectManager objectManager;

    protected IVersionManager versionManager;

    public ObjectStore(Path rootDir, String indexFileName, String objectDirName, IStorageAdapter storageAdapter)
            throws InputOutputException {
        this.rootDir = rootDir;
        this.objectManager = new ObjectManager(indexFileName, objectDirName, storageAdapter);
        this.versionManager = new VersionManager(this.objectManager);
    }

    public void onCreateFile(String relativePath, String contentHash)
            throws InputOutputException {
        logger.debug("Creating object for " + relativePath);

        Path relativePathToWatchedDir = Paths.get(relativePath);
        Path absolutePathOnFs = this.rootDir.resolve(relativePathToWatchedDir);

        // filename.txt
        // myPath/to/filename.txt
        int idx = relativePath.indexOf(relativePathToWatchedDir.getFileName().toString());
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
                pathToFileWithoutFilename,
                pathType,
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
        this.objectManager.removeObject(Hash.hash(Config.DEFAULT.getHashingAlgorithm(), relativePath));
    }

    public void onMoveFile(String oldRelativePath, String newRelativePath)
            throws InputOutputException {
        logger.debug("Moving object for " + oldRelativePath);

        // move all objects for the directories content
        // THIS SHOULD NOT BE NEEDED IF ADDDIRECTORYCONTENTMODIFIER IS USED
//        File oldFile = this.rootDir.resolve(oldRelativePath).toFile();
//        if (oldFile.exists() && oldFile.isDirectory() && null != oldFile.listFiles()) {
//            Path newPath = Paths.get(newRelativePath);
//            for (File child : Arrays.asList(oldFile.listFiles())) {
//                Path relativeChildToParentPath = Paths.get(oldRelativePath).relativize(child.toPath());
//                logger.trace("Moving child object for move from " + this.rootDir.relativize(child.toPath()).toString() + " to " + newPath.resolve(relativeChildToParentPath).toString());
//                this.onMoveFile(this.rootDir.relativize(child.toPath()).toString(), newPath.resolve(relativeChildToParentPath).toString());
//            }
//        }

        PathObject oldObject = this.objectManager.getObject(Hash.hash(Config.DEFAULT.getHashingAlgorithm(), oldRelativePath));
        PathObject newObject = new PathObject(
                oldObject.getName(),
                Naming.getPathWithoutFileName(oldObject.getName(), newRelativePath),
                oldObject.getPathType(),
                oldObject.isShared(),
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
