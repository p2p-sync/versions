package org.rmatil.sync.version.core.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.rmatil.sync.version.api.PathType;

import java.util.List;

public class PathObject {

    protected static Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();;

    protected String name;

    protected String path;

    protected PathType pathType;

    protected boolean isShared;

    protected List<Sharer> sharers;

    protected List<Version> versions;

    public PathObject(String name, String path, PathType pathType, boolean isShared, List<Sharer> sharers, List<Version> versions) {
        this.name = name;
        this.path = path;
        this.pathType = pathType;
        this.isShared = isShared;
        this.sharers = sharers;
        this.versions = versions;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public String getAbsolutePath() {
        return this.getPath() + "/" + this.getName();
    }

    public PathType getPathType() {
        return pathType;
    }

    public boolean isShared() {
        return isShared;
    }

    public List<Sharer> getSharers() {
        return sharers;
    }

    public List<Version> getVersions() {
        return versions;
    }

    public String toJson() {
        return gson.toJson(this, PathObject.class);
    }

    public static PathObject fromJson(String json) {
        return gson.fromJson(json, PathObject.class);
    }
}
