package org.rmatil.sync.version.core.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Map;
import java.util.UUID;

public class Index {

    protected static Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();;

    protected Map<String, String> paths;

    protected Map<UUID, String> sharedPaths;

    public Index(Map<String, String> paths, Map<UUID, String> sharedPaths) {
        this.paths = paths;
        this.sharedPaths = sharedPaths;

    }

    public void addPath(String pathToFile, String hashOfFilePath) {
        this.paths.put(pathToFile, hashOfFilePath);
    }

    public void removePath(String pathToFile) {
        this.paths.remove(pathToFile);
    }

    public Map<String, String> getPaths() {
        return this.paths;
    }

    public void addSharedPath(UUID sharedFileId, String hashOfFilePath) {
        this.sharedPaths.put(sharedFileId, hashOfFilePath);
    }

    public void removeSharedPath(UUID sharedFileId) {
        this.sharedPaths.remove(sharedFileId);
    }

    public Map<UUID, String> getSharedPaths() {
        return sharedPaths;
    }

    public String toJson() {
        return gson.toJson(this, Index.class);
    }

    public static Index fromJson(String json) {
        return gson.fromJson(json, Index.class);
    }
}
