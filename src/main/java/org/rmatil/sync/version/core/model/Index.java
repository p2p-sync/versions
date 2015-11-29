package org.rmatil.sync.version.core.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Map;

public class Index {

    protected static Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();;

    protected Map<String, String> paths;

    public Index(Map<String, String> paths) {
        this.paths = paths;
    }

    public void addPath(String pathToFile, String hashOfFilePath) {
        this.paths.put(pathToFile, hashOfFilePath);
    }

    public void removePath(String pathToFile) {
        this.paths.remove(pathToFile);
    }

    public Map<String, String> getPaths() {
        return paths;
    }

    public String toJson() {
        return gson.toJson(this, Index.class);
    }

    public static Index fromJson(String json) {
        return gson.fromJson(json, Index.class);
    }
}
