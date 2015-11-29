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

    public List<Version> getVersions(String pathToFile)
            throws InputOutputException {
        String fileNameHash = Hash.hash(Config.DEFAULT.getHashingAlgorithm(), pathToFile);

        PathObject pathObject = this.objectManager.getObject(fileNameHash);
        return pathObject.getVersions();
    }

    public void addVersion(Version version, String pathToFile)
            throws InputOutputException {
        String fileNameHash = Hash.hash(Config.DEFAULT.getHashingAlgorithm(), pathToFile);

        PathObject pathObject = this.objectManager.getObject(fileNameHash);
        pathObject.getVersions().add(version);
        this.objectManager.writeObject(pathObject);
    }

    public void removeVersion(Version version, String pathToFile)
            throws InputOutputException {
        String fileNameHash = Hash.hash(Config.DEFAULT.getHashingAlgorithm(), pathToFile);

        PathObject pathObject = this.objectManager.getObject(fileNameHash);
        // requires to overwrite equals
        pathObject.getVersions().remove(version);
        this.objectManager.writeObject(pathObject);
    }
}
