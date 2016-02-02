package org.rmatil.sync.version.core;

import org.rmatil.sync.commons.hashing.Hash;
import org.rmatil.sync.persistence.exceptions.InputOutputException;
import org.rmatil.sync.version.api.AccessType;
import org.rmatil.sync.version.api.IObjectManager;
import org.rmatil.sync.version.api.ISharerManager;
import org.rmatil.sync.version.config.Config;
import org.rmatil.sync.version.core.model.PathObject;
import org.rmatil.sync.version.core.model.Sharer;

import java.util.ArrayList;
import java.util.List;
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
    public synchronized void addSharer(String username, AccessType accessType, String pathToFile)
            throws InputOutputException {
        String fileNameHash = Hash.hash(Config.DEFAULT.getHashingAlgorithm(), pathToFile);

        String firstSharingHistoryEntry = Hash.hash(Config.DEFAULT.getHashingAlgorithm(), accessType.name());

        List<String> sharingHistory = new ArrayList<>();
        sharingHistory.add(firstSharingHistoryEntry);
        Sharer sharer = new Sharer(
                username,
                accessType,
                sharingHistory
        );

        PathObject pathObject = this.objectManager.getObject(fileNameHash);
        pathObject.setIsShared(true);
        pathObject.getSharers().add(sharer);
        this.objectManager.writeObject(pathObject);
    }

    @Override
    public synchronized void removeSharer(String username, String pathToFile)
            throws InputOutputException {
        String fileNameHash = Hash.hash(Config.DEFAULT.getHashingAlgorithm(), pathToFile);

        PathObject pathObject = this.objectManager.getObject(fileNameHash);

        boolean isLastSharerForPath = true;
        Sharer sharer = null;
        for (Sharer entry : pathObject.getSharers()) {
            if (entry.getUsername().equals(username)) {
                sharer = entry;
            } else {
                isLastSharerForPath = false;
            }
        }

        if (null == sharer) {
            throw new InputOutputException("Can not remove sharer " + username + " since he is not present in the list");
        }

        // if the sharer is the last sharer for the file, we can remove the shared flag
        if (isLastSharerForPath) {
            pathObject.setIsShared(false);
            pathObject.setOwner(null);
        }

        String nextSharingHistoryEntry = "";
        // make a hash of all previously history entries
        for (String shareHistory : sharer.getSharingHistory()) {
            nextSharingHistoryEntry = Hash.hash(Config.DEFAULT.getHashingAlgorithm(), nextSharingHistoryEntry + shareHistory);
        }

        // now add the new state to the history
        nextSharingHistoryEntry = Hash.hash(Config.DEFAULT.getHashingAlgorithm(), nextSharingHistoryEntry + AccessType.ACCESS_REMOVED.name());
        sharer.getSharingHistory().add(nextSharingHistoryEntry);
        sharer.setAccessType(AccessType.ACCESS_REMOVED);

        pathObject.getSharers().add(sharer);
        this.objectManager.writeObject(pathObject);
    }

    @Override
    public void addOwner(String username, String pathToFile)
            throws InputOutputException {
        PathObject pathObject = this.objectManager.getObjectForPath(pathToFile);
        pathObject.setOwner(username);
        this.objectManager.writeObject(pathObject);
    }

    @Override
    public void removeOwner(String pathToFile)
            throws InputOutputException {
        PathObject pathObject = this.objectManager.getObjectForPath(pathToFile);

        pathObject.setOwner(null);
        this.objectManager.writeObject(pathObject);
    }

    @Override
    public String getOwner(String pathToFile)
            throws InputOutputException {
        PathObject pathObject = this.objectManager.getObjectForPath(pathToFile);
        return pathObject.getOwner();
    }

    @Override
    public IObjectManager getObjectManager() {
        return this.objectManager;
    }
}
