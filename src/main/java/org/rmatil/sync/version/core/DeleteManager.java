package org.rmatil.sync.version.core;

import org.rmatil.sync.commons.hashing.Hash;
import org.rmatil.sync.persistence.exceptions.InputOutputException;
import org.rmatil.sync.version.api.DeleteType;
import org.rmatil.sync.version.api.IDeleteManager;
import org.rmatil.sync.version.api.IObjectManager;
import org.rmatil.sync.version.config.Config;
import org.rmatil.sync.version.core.model.Delete;
import org.rmatil.sync.version.core.model.PathObject;

import java.util.List;

public class DeleteManager implements IDeleteManager {

    protected IObjectManager objectManager;

    public DeleteManager(IObjectManager objectManager) {
        this.objectManager = objectManager;
    }

    @Override
    public synchronized Delete getDelete(String pathToFile)
            throws InputOutputException {
        PathObject pathObject = this.objectManager.getObjectForPath(pathToFile);

        return pathObject.getDeleted();
    }

    @Override
    public synchronized void setIsDeleted(String pathToFile)
            throws InputOutputException {
        this.addChange(pathToFile, DeleteType.DELETED);
    }

    @Override
    public synchronized void setIsExistent(String pathToFile)
            throws InputOutputException {
        this.addChange(pathToFile, DeleteType.EXISTENT);
    }

    protected synchronized void addChange(String pathToFile, DeleteType deleteType)
            throws InputOutputException {
        PathObject pathObject = this.objectManager.getObjectForPath(pathToFile);
        pathObject.getDeleted().setDeleteType(deleteType);

        List<String> deleteHistory = pathObject.getDeleted().getDeleteHistory();

        String nextDeleteHistoryEntry = "";
        for (String entry : deleteHistory) {
            nextDeleteHistoryEntry = Hash.hash(Config.DEFAULT.getHashingAlgorithm(), nextDeleteHistoryEntry + entry);
        }

        // add latest change too
        nextDeleteHistoryEntry = Hash.hash(Config.DEFAULT.getHashingAlgorithm(), nextDeleteHistoryEntry + deleteType.name());
        deleteHistory.add(nextDeleteHistoryEntry);

        this.objectManager.writeObject(pathObject);
    }
}
