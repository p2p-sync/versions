package org.rmatil.sync.version.core.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Map;
import java.util.UUID;

public class Index {

    protected static Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();;

    protected Map<String, UUID> pathIdentifiers;

    protected Map<UUID, String> paths;

    public Index(Map<String, UUID> pathIdentifiers, Map<UUID, String> paths) {
        this.pathIdentifiers = pathIdentifiers;
        this.paths = paths;

    }

    public void addPath(String pathToFile, String hashOfFilePath) {

        UUID fileId = this.getPathIdentifiers().get(pathToFile);

        if (null == fileId) {
            fileId = UUID.randomUUID();
            this.paths.put(fileId, hashOfFilePath);
        }

        this.pathIdentifiers.put(pathToFile, fileId);
    }

    public void removePath(String pathToFile) {
        UUID fileId = this.pathIdentifiers.get(pathToFile);

        this.pathIdentifiers.remove(pathToFile);

        if (null != fileId) {
            this.paths.remove(fileId);
        }
    }

    public Map<String, UUID> getPathIdentifiers() {
        return this.pathIdentifiers;
    }

    public Map<UUID, String> getPaths() {
        return paths;
    }

    public String toJson() {
        return gson.toJson(this, Index.class);
    }

    public static Index fromJson(String json) {
        return gson.fromJson(json, Index.class);
    }
}
