package org.rmatil.sync.version.core;

import org.rmatil.sync.commons.hashing.Hash;
import org.rmatil.sync.persistence.exceptions.InputOutputException;
import org.rmatil.sync.version.api.IObjectManager;
import org.rmatil.sync.version.api.ISharerManager;
import org.rmatil.sync.version.config.Config;
import org.rmatil.sync.version.core.model.PathObject;
import org.rmatil.sync.version.core.model.Sharer;

import java.util.Set;

public class SharerManager implements ISharerManager {

    protected IObjectManager objectManager;

    public SharerManager(IObjectManager objectManager) {
        this.objectManager = objectManager;
    }

    @Override
    public synchronized Set<Sharer> getSharer(String pathToFile)
            throws InputOutputException {
        String fileNameHash = Hash.hash(Config.DEFAULT.getHashingAlgorithm(), pathToFile);

        PathObject pathObject = this.objectManager.getObject(fileNameHash);
        return pathObject.getSharers();
    }

    @Override
    public synchronized void addSharer(Sharer sharer, String pathToFile)
            throws InputOutputException {
        String fileNameHash = Hash.hash(Config.DEFAULT.getHashingAlgorithm(), pathToFile);

        PathObject pathObject = this.objectManager.getObject(fileNameHash);
        pathObject.getSharers().add(sharer);
        this.objectManager.writeObject(pathObject);
    }

    @Override
    public synchronized void removeSharer(Sharer sharer, String pathToFile)
            throws InputOutputException {
        String fileNameHash = Hash.hash(Config.DEFAULT.getHashingAlgorithm(), pathToFile);

        PathObject pathObject = this.objectManager.getObject(fileNameHash);
        // requires to overwrite equals
        pathObject.getSharers().remove(sharer);
        this.objectManager.writeObject(pathObject);
    }

    @Override
    public IObjectManager getObjectManager() {
        return this.objectManager;
    }
}
