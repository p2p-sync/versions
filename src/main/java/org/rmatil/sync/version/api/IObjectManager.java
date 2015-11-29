package org.rmatil.sync.version.api;

import org.rmatil.sync.persistence.exceptions.InputOutputException;
import org.rmatil.sync.version.core.model.Index;
import org.rmatil.sync.version.core.model.PathObject;

public interface IObjectManager {

    void writeObject(PathObject path)
            throws InputOutputException;

    PathObject getObject(String fileNameHash)
            throws InputOutputException;

    void removeObject(String fileNameHash)
            throws InputOutputException;

    Index getIndex();

    String getIndexFileName();

}
