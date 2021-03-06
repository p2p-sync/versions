package org.rmatil.sync.version.core;

import org.rmatil.sync.commons.hashing.Hash;
import org.rmatil.sync.persistence.exceptions.InputOutputException;
import org.rmatil.sync.version.api.IObjectManager;
import org.rmatil.sync.version.api.IVersionManager;
import org.rmatil.sync.version.config.Config;
import org.rmatil.sync.version.core.model.PathObject;
import org.rmatil.sync.version.core.model.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class VersionManager implements IVersionManager {

    private static final Logger logger = LoggerFactory.getLogger(VersionManager.class);

    protected IObjectManager objectManager;

    /**
     * @param objectManager The object manager to get access to the object store
     */
    public VersionManager(IObjectManager objectManager) {
        this.objectManager = objectManager;
    }

    public synchronized List<Version> getVersions(String pathToFile)
            throws InputOutputException {
        String fileNameHash = Hash.hash(Config.DEFAULT.getHashingAlgorithm(), pathToFile);

        PathObject pathObject = this.objectManager.getObject(fileNameHash);
        return pathObject.getVersions();
    }

    public synchronized void addVersion(Version version, String pathToFile)
            throws InputOutputException {
        String fileNameHash = Hash.hash(Config.DEFAULT.getHashingAlgorithm(), pathToFile);

        PathObject pathObject = this.objectManager.getObject(fileNameHash);

        if (! pathObject.getVersions().isEmpty()) {
            // only add a version if the last is not the same
            Version lastVersion = pathObject.getVersions().get(Math.max(0, pathObject.getVersions().size() - 1));
            if (! lastVersion.getHash().equals(version.getHash())) {
                pathObject.getVersions().add(version);
            }
        } else {
            pathObject.getVersions().add(version);
        }

        this.objectManager.writeObject(pathObject);
    }

    public synchronized void removeVersion(Version version, String pathToFile)
            throws InputOutputException {
        String fileNameHash = Hash.hash(Config.DEFAULT.getHashingAlgorithm(), pathToFile);

        PathObject pathObject = this.objectManager.getObject(fileNameHash);
        // requires to overwrite equals
        pathObject.getVersions().remove(version);
        this.objectManager.writeObject(pathObject);
    }

    public IObjectManager getObjectManager() {
        return this.objectManager;
    }
}
