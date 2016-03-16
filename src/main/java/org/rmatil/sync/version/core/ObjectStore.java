package org.rmatil.sync.version.core;

import org.rmatil.sync.commons.hashing.Hash;
import org.rmatil.sync.commons.path.Naming;
import org.rmatil.sync.persistence.api.StorageType;
import org.rmatil.sync.persistence.core.tree.ITreeStorageAdapter;
import org.rmatil.sync.persistence.core.tree.TreePathElement;
import org.rmatil.sync.persistence.exceptions.InputOutputException;
import org.rmatil.sync.version.api.*;
import org.rmatil.sync.version.config.Config;
import org.rmatil.sync.version.core.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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

    protected ITreeStorageAdapter folderStorageAdapter;

    protected ITreeStorageAdapter objectStoreStorageAdapter;

    protected IObjectManager objectManager;

    protected IVersionManager versionManager;

    protected ISharerManager sharerManager;

    protected IDeleteManager deleteManager;

    public ObjectStore(ITreeStorageAdapter folderStorageAdapter, String indexFileName, String objectDirName, ITreeStorageAdapter objectStoreStorageAdapter)
            throws InputOutputException {
        this.folderStorageAdapter = folderStorageAdapter;
        this.objectStoreStorageAdapter = objectStoreStorageAdapter;
        this.objectManager = new ObjectManager(indexFileName, objectDirName, objectStoreStorageAdapter);
        this.versionManager = new VersionManager(this.objectManager);
        this.sharerManager = new SharerManager(this.objectManager);
        this.deleteManager = new DeleteManager(this.objectManager);
    }

    @Override
    public void sync()
            throws InputOutputException {
        this.sync(new ArrayList<>());
    }

    @Override
    public void sync(List<String> ignoredFiles)
            throws InputOutputException {
        // first remove all object which are not present anymore on the storage
        for (Map.Entry<String, String> entry : this.objectManager.getIndex().getPaths().entrySet()) {
            // flag the file as deleted
            TreePathElement treePathElement = new TreePathElement(entry.getKey());
            if (! this.folderStorageAdapter.exists(StorageType.FILE, treePathElement) &&
                    ! this.folderStorageAdapter.exists(StorageType.DIRECTORY, treePathElement)) {
                this.onRemoveFile(entry.getKey());
            }
        }

        // now insert or update the files on storage
        List<TreePathElement> files = this.folderStorageAdapter.getDirectoryContents(
                new TreePathElement("/")
        );

        // recreate objects but ignore the sync folder
        TreePathElement syncFolder = this.objectStoreStorageAdapter.getRootDir();
        TreePathElement relSyncFolder = new TreePathElement(Paths.get(this.folderStorageAdapter.getRootDir().getPath()).relativize(Paths.get(syncFolder.getPath())).toString());
        for (TreePathElement entry : files) {
            if (entry.getPath().equals(relSyncFolder.getPath()) ||
                    entry.getPath().startsWith(relSyncFolder.getPath()) ||
                    ignoredFiles.contains(entry.getPath())) {
                // ignore sync folder and all its contents
                logger.trace("Ignoring sync folder from being created in the index");
                continue;
            }

            this.syncChild(entry);
        }
    }

    @Override
    public void syncFile(TreePathElement file)
            throws InputOutputException {
        // first remove object to force recreation
        this.getObjectManager().removeObject(
                Hash.hash(
                        Config.DEFAULT.getHashingAlgorithm(),
                        file.getPath()
                )
        );

        this.syncChild(file);
    }

    protected void syncChild(TreePathElement file)
            throws InputOutputException {

        // recalculate the hash of the file
        String hash = null;
        try {
            if (this.folderStorageAdapter.isFile(file) || this.folderStorageAdapter.isDir(file)) {
                Path absoluteFile = Paths.get(this.folderStorageAdapter.getRootDir().getPath()).resolve(file.getPath());
                hash = Hash.hash(
                        Config.DEFAULT.getHashingAlgorithm(),
                        absoluteFile.toFile()
                );
            }
        } catch (IOException e1) {
            logger.error("Could not create path object for file " + file.getPath() + ". Message: " + e1.getMessage());
        }

        try {
            // throws an exception if path does not exist
            PathObject oldObject = this.objectManager.getObjectForPath(file.getPath());

            // just update the content hash
            this.onModifyFile(file.getPath(), hash);
        } catch (InputOutputException e) {
            // file does not exist yet, so we create it
            logger.debug("No object stored for file " + file.getPath() + ". Creating...");
            this.onCreateFile(file.getPath(), hash);
        }
    }

    @Override
    public void onCreateFile(String relativePath, String contentHash)
            throws InputOutputException {
        logger.debug("Creating object for " + relativePath);

        TreePathElement element = new TreePathElement(relativePath);

        PathType pathType = null;
        if (this.folderStorageAdapter.isDir(element)) {
            pathType = PathType.DIRECTORY;
        } else if (this.folderStorageAdapter.isFile(element)) {
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
