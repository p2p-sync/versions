package org.rmatil.sync.version.core;

import org.rmatil.sync.commons.hashing.Hash;
import org.rmatil.sync.commons.path.Naming;
import org.rmatil.sync.persistence.api.IStorageAdapter;
import org.rmatil.sync.persistence.exceptions.InputOutputException;
import org.rmatil.sync.version.api.*;
import org.rmatil.sync.version.config.Config;
import org.rmatil.sync.version.core.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ObjectStore implements IObjectStore {

    private final static Logger logger = LoggerFactory.getLogger(ObjectStore.class);

    /**
     * The path type of merged file sharedPaths
     */
    public enum MergedObjectType {
        /**
         * The path was deleted on the remote, but not in this object store
         */
        DELETED,

        /**
         * The path was changed, i.e. has a different newer version
         */
        CHANGED,

        /**
         * The path that created a conflict
         */
        CONFLICT
    }

    protected Path rootDir;

    protected IStorageAdapter storageAdapter;

    protected IObjectManager objectManager;

    protected IVersionManager versionManager;

    protected ISharerManager sharerManager;

    protected IDeleteManager deleteManager;

    public ObjectStore(Path rootDir, String indexFileName, String objectDirName, IStorageAdapter storageAdapter)
            throws InputOutputException {
        this.rootDir = rootDir;
        this.objectManager = new ObjectManager(indexFileName, objectDirName, storageAdapter);
        this.versionManager = new VersionManager(this.objectManager);
        this.sharerManager = new SharerManager(this.objectManager);
        this.deleteManager = new DeleteManager(this.objectManager);
        this.storageAdapter = storageAdapter;
    }

    @Override
    public void sync(File rootSyncDir)
            throws InputOutputException {
        this.sync(rootSyncDir, new ArrayList<>());
    }

    @Override
    public void sync(File rootSyncDir, List<String> ignoredFiles)
            throws InputOutputException {
        if (null == rootSyncDir || ! rootSyncDir.exists()) {
            throw new InputOutputException("Can not sync index. Root of synchronized folder does not exist.");
        }

        // first remove all object which are not present anymore on the storage
        for (Map.Entry<String, String> entry : this.objectManager.getIndex().getPaths().entrySet()) {
            // flag the file as deleted
            if (! Files.exists(this.rootDir.resolve(entry.getKey()))) {
                this.onRemoveFile(entry.getKey());
            }
        }

        // now insert or update the files on storage
        File[] files = rootSyncDir.listFiles();
        if (null == files) {
            logger.info("Abort sync of object store, no files in given directory");
            return;
        }

        // recreate objects
        for (File file : files) {
            Path relativeSyncFolder = this.rootDir.relativize(this.storageAdapter.getRootDir());
            if (file.getAbsolutePath().equals(this.rootDir.resolve(relativeSyncFolder).toFile().getAbsolutePath()) ||
                    ignoredFiles.contains(this.rootDir.relativize(file.toPath()).toString())) {
                // ignore sync folder
                logger.trace("Ignoring sync folder from being created in the index");
                continue;
            }

            this.syncChild(file);
        }
    }

    @Override
    public void syncFile(File file)
            throws InputOutputException {
        if (! file.exists()) {
            throw new InputOutputException(file.getPath() + " (No such file or directory)");
        }

        // first remove object to force recreation
        Path relativePathToRootDir = this.rootDir.relativize(file.toPath());
        this.getObjectManager().removeObject(Hash.hash(Config.DEFAULT.getHashingAlgorithm(), relativePathToRootDir.toString()));

        this.syncChild(file);
    }

    protected void syncChild(File file)
            throws InputOutputException {
        Path relativePathToRootDir = this.rootDir.relativize(file.toPath());

        // recalculate the hash of the file
        String hash = null;
        try {
            if (file.isFile() || file.isDirectory()) {
                hash = Hash.hash(Config.DEFAULT.getHashingAlgorithm(), file);
            }
        } catch (IOException e1) {
            logger.error("Could not create path object for file " + relativePathToRootDir.toString() + ". Message: " + e1.getMessage());
        }

        try {
            // throws an exception if path does not exist
            PathObject oldObject = this.objectManager.getObject(Hash.hash(Config.DEFAULT.getHashingAlgorithm(), relativePathToRootDir.toString()));

            // just update the content hash
            this.onModifyFile(relativePathToRootDir.toString(), hash);
        } catch (InputOutputException e) {
            // file does not exist yet, so we create it
            logger.debug("No object stored for file " + relativePathToRootDir.toString() + ". Creating...");
            this.onCreateFile(relativePathToRootDir.toString(), hash);
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

    @Override
    public void onCreateFile(String relativePath, String contentHash)
            throws InputOutputException {
        logger.debug("Creating object for " + relativePath);

        Path absolutePathOnFs = this.rootDir.resolve(relativePath);

        PathType pathType = null;
        if (absolutePathOnFs.toFile().isDirectory()) {
            pathType = PathType.DIRECTORY;
        } else if (absolutePathOnFs.toFile().isFile()) {
            pathType = PathType.FILE;
        }

        this.onCreateFile(relativePath, pathType, contentHash);
    }

    protected void onCreateFile(String relativePath, PathType pathType, String contentHash)
            throws InputOutputException {

        Path relativePathToWatchedDir = Paths.get(relativePath);

        // filename.txt
        // myPath/to/filename.txt
        int idx = relativePath.lastIndexOf(relativePathToWatchedDir.getFileName().toString());
        String pathToFileWithoutFilename = relativePath.substring(0, idx);

        if (pathToFileWithoutFilename.endsWith("/")) {
            pathToFileWithoutFilename = pathToFileWithoutFilename.substring(0, pathToFileWithoutFilename.length() - 1);
        }

        Version v1 = new Version(contentHash);
        List<Version> versions = new ArrayList<>();
        versions.add(v1);

        // to ensure, that elements which are located within a shared
        // directory are also shared, we have to check the sharing
        // properties of its direct parent
        boolean isShared = false;
        Set<Sharer> sharers = new HashSet<>();

        String parent = Naming.getParentPath(relativePath);

        if (null != parent && ! "/".equals(parent)) {
            try {
                PathObject parentObject = this.objectManager.getObjectForPath(parent);

                if (parentObject.isShared()) {
                    isShared = true;
                    sharers = parentObject.getSharers();
                }

            } catch (InputOutputException e) {
                logger.error("Could not fetch parent object for path " + parent + " to check for sharers: " + e.getMessage());
            }
        }

        PathObject oldObject;
        List<String> deleteHistory = new ArrayList<>();
        try {
            oldObject = this.objectManager.getObjectForPath(relativePathToWatchedDir.toString());
            // replace delete history
            deleteHistory = oldObject.getDeleted().getDeleteHistory();
        } catch (InputOutputException e) {
            // no object is stored on this path yet
        }

        PathObject pathObject = new PathObject(
                relativePathToWatchedDir.getFileName().toString(),
                pathToFileWithoutFilename,
                pathType,
                null,
                isShared,
                new Delete(DeleteType.EXISTENT, deleteHistory),
                null,
                sharers,
                versions
        );

        this.objectManager.writeObject(pathObject);
        this.deleteManager.setIsExistent(relativePathToWatchedDir.toString());
    }

    @Override
    public void onModifyFile(String relativePath, String contentHash)
            throws InputOutputException {
        logger.debug("Modifying object for " + relativePath);
        this.versionManager.addVersion(new Version(contentHash), relativePath);
    }

    @Override
    public void onRemoveFile(String relativePath)
            throws InputOutputException {
        logger.debug("Removing object for " + relativePath);

        // just setting the deleted flag

        PathObject object = this.objectManager.getObject(Hash.hash(Config.DEFAULT.getHashingAlgorithm(), relativePath));
        PathObject deletedObject = new PathObject(
                object.getName(),
                Naming.getPathWithoutFileName(object.getName(), relativePath),
                object.getPathType(),
                object.getAccessType(),
                object.isShared(),
                object.getDeleted(),
                null, // reset
                new HashSet<>(), // reset
                object.getVersions()
        );

        this.objectManager.writeObject(deletedObject);
        this.deleteManager.setIsDeleted(relativePath);
    }

    @Override
    public void onMoveFile(String oldRelativePath, String newRelativePath)
            throws InputOutputException {
        logger.debug("Moving object for " + oldRelativePath);

        PathObject oldObject = this.objectManager.getObject(Hash.hash(Config.DEFAULT.getHashingAlgorithm(), oldRelativePath));
        PathObject newObject = new PathObject(
                oldObject.getName(),
                Naming.getPathWithoutFileName(oldObject.getName(), newRelativePath),
                oldObject.getPathType(),
                oldObject.getAccessType(),
                oldObject.isShared(),
                oldObject.getDeleted(),
                oldObject.getOwner(),
                oldObject.getSharers(),
                oldObject.getVersions()
        );

        this.objectManager.writeObject(newObject);
        this.objectManager.removeObject(Hash.hash(Config.DEFAULT.getHashingAlgorithm(), oldRelativePath));

    }

    @Override
    public IObjectManager getObjectManager() {
        return this.objectManager;
    }

    @Override
    public ISharerManager getSharerManager() {
        return this.sharerManager;
    }

    @Override
    public IDeleteManager getDeleteManager() {
        return this.deleteManager;
    }

    @Override
    public HashMap<MergedObjectType, Set<String>> mergeObjectStore(IObjectStore otherObjectStore)
            throws InputOutputException {
        HashMap<MergedObjectType, Set<String>> missingOrOutdatedPaths = new HashMap<>();
        missingOrOutdatedPaths.put(MergedObjectType.CHANGED, new HashSet<>());
        missingOrOutdatedPaths.put(MergedObjectType.DELETED, new HashSet<>());
        missingOrOutdatedPaths.put(MergedObjectType.CONFLICT, new HashSet<>());

        Index ourIndex = this.getObjectManager().getIndex();
        Index otherIndex = otherObjectStore.getObjectManager().getIndex();

        // check if we have the file
        for (Map.Entry<String, String> entry : otherIndex.getPaths().entrySet()) {
            if (null != ourIndex.getPaths().get(entry.getKey())) {
                // ok, we got the file too, now check the version and if the file should be deleted
                String hashToFile = otherIndex.getPaths().get(entry.getKey());
                PathObject otherPathObject = otherObjectStore.getObjectManager().getObject(hashToFile);
                PathObject ourPathObject = this.getObjectManager().getObject(hashToFile);

                // check if we should have deleted that file
                if (! ourPathObject.getDeleted().getDeleteType().equals(otherPathObject.getDeleted().getDeleteType())) {
                    // check whether the other delete history is greater
                    if (ourPathObject.getDeleted().getDeleteHistory().size() < otherPathObject.getDeleted().getDeleteHistory().size()) {
                        if (DeleteType.DELETED == otherPathObject.getDeleted().getDeleteType()) {
                            missingOrOutdatedPaths.get(MergedObjectType.DELETED).add(entry.getKey());
                            // reset owner and sharer
                            this.onRemoveFile(entry.getKey());
                        } else {
                            // if the file exists (again), we have to fetch it
                            missingOrOutdatedPaths.get(MergedObjectType.CHANGED).add(entry.getKey());
                        }

                        // it is -> we have to get the state of the other
                        // but use the history of the other
                        ourPathObject.setDeleted(otherPathObject.getDeleted());

                        this.objectManager.writeObject(ourPathObject);
                        continue;
                    }
                } else {
                    // final state is equal, check that we have the same history
                    if (ourPathObject.getDeleted().getDeleteHistory().size() < otherPathObject.getDeleted().getDeleteHistory().size()) {
                        ourPathObject.setDeleted(otherPathObject.getDeleted());
                        this.objectManager.writeObject(ourPathObject);
                    }
                }

                List<Version> versions = this.getObjectManager().getObject(hashToFile).getVersions();
                Version lastVersion = versions.get(Math.max(0, versions.size() - 1));

                // check whether the other version list has our version, if not
                // then we have a more recent version
                if (otherPathObject.getVersions().contains(lastVersion)) {
                    // check whether our version is earlier in the list than the other
                    if (versions.indexOf(lastVersion) < otherPathObject.getVersions().indexOf(lastVersion)) {
                        // we have to copy the last other version
                        versions.add(otherPathObject.getVersions().get(Math.max(0, otherPathObject.getVersions().size() - 1)));
                        // add the path to the file to the outdated files
                        missingOrOutdatedPaths.get(MergedObjectType.CHANGED).add(entry.getKey());
                    }

                    // add all missing other versions between our last one and the last one of the other object store
                    if (versions.size() < otherPathObject.getVersions().size()) {
                        versions.addAll(otherPathObject.getVersions().subList(
                                otherPathObject.getVersions().indexOf(lastVersion),
                                otherPathObject.getVersions().size() - 1
                        ));
                        missingOrOutdatedPaths.get(MergedObjectType.CHANGED).add(entry.getKey());
                    }
                } else if (otherPathObject.getVersions().size() > 0) {
                    // there is a conflict on the file
                    // -> do only create a conflict for non directory paths
                    if (PathType.DIRECTORY != otherPathObject.getPathType()) {
                        missingOrOutdatedPaths.get(MergedObjectType.CONFLICT).add(entry.getKey());
                    }
                }

                // merge sharers
                Set<Sharer> sharers = ourPathObject.getSharers();
                Set<Sharer> otherSharers = otherPathObject.getSharers();

                for (Sharer ownSharer : sharers) {
                    for (Sharer otherSharer : otherSharers) {
                        if (ownSharer.getUsername().equals(otherSharer.getUsername())) {
                            // compare history
                            if (ownSharer.getSharingHistory().size() < otherSharer.getSharingHistory().size()) {
                                // we replace the other sharing history with our one
                                ownSharer.setSharingHistory(otherSharer.getSharingHistory());
                            }

                            break;
                        }
                    }
                }

                // now add all sharers we do not have
                for (Sharer otherSharer : otherSharers) {
                    boolean hasOtherSharer = false;
                    for (Sharer ourSharer : sharers) {
                        if (otherSharer.getUsername().equals(ourSharer.getUsername())) {
                            hasOtherSharer = true;
                            break;
                        }
                    }

                    // add other sharer if he does not exist yet
                    if (! hasOtherSharer) {
                        sharers.add(otherSharer);
                    }
                }

                ourPathObject.setSharers(sharers);

                // merge owner
                if (null == ourPathObject.getOwner() && null != otherPathObject.getOwner()) {
                    // check if the other client has an owner
                    ourPathObject.setOwner(otherPathObject.getOwner());
                }

                // update changes
                this.getObjectManager().writeObject(ourPathObject);

            } else {
                // we do not have the file yet, so check whether it should be deleted (flag)
                // or if we just do not have it and have to request it later on
                String hashToFile = otherIndex.getPaths().get(entry.getKey());
                PathObject otherPathObject = otherObjectStore.getObjectManager().getObject(hashToFile);

                // no need to remove the file from our index since it did not exist anyway
                if (DeleteType.EXISTENT == otherPathObject.getDeleted().getDeleteType()) {
                    // the file was not deleted, so we have to add it to our object store
                    int versionCtr = 0;
                    for (Version version : otherPathObject.getVersions()) {
                        if (0 == versionCtr) {
                            this.onCreateFile(entry.getKey(), otherPathObject.getPathType(), version.getHash());
                        } else {
                            this.onModifyFile(entry.getKey(), version.getHash());
                        }

                        versionCtr++;
                    }

                    // add the path to the file to the missing files
                    missingOrOutdatedPaths.get(MergedObjectType.CHANGED).add(entry.getKey());
                } else {
                    // the file was deleted
                    PathObject deletedPathObject = new PathObject(
                            otherPathObject.getName(),
                            otherPathObject.getPath(),
                            otherPathObject.getPathType(),
                            otherPathObject.getAccessType(),
                            otherPathObject.isShared(),
                            otherPathObject.getDeleted(),
                            otherPathObject.getOwner(),
                            otherPathObject.getSharers(),
                            otherPathObject.getVersions()
                    );

                    // add the state too for the deleted file
                    this.objectManager.writeObject(deletedPathObject);
                }
            }
        }


        return missingOrOutdatedPaths;
    }
}
