package org.rmatil.sync.version.core.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.rmatil.sync.version.api.PathType;

import java.util.ArrayList;
import java.util.List;

public class PathObject {

    protected static Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
    ;

    /**
     * The name of the directory or file (without a path)
     */
    protected String name;

    /**
     * The path to the file or directory without the filename
     */
    protected String path;

    /**
     * The type of the file, e.g directory or file
     */
    protected PathType pathType;

    /**
     * Whether this object is shared
     */
    protected boolean isShared;

    /**
     * A list of sharers of this file
     */
    protected List<Sharer> sharers;

    /**
     * A list of versions of this file
     */
    protected List<Version> versions;

    /**
     * @param name     The name of the file or directory (without the path to it)
     * @param path     The path to the file or directory (without the name of it)
     * @param pathType The type of the file
     * @param isShared Whether this file is shared
     * @param sharers  A list of sharers
     * @param versions A list of versions
     */
    public PathObject(String name, String path, PathType pathType, boolean isShared, List<Sharer> sharers, List<Version> versions) {
        this.name = name;
        this.path = path;
        this.pathType = pathType;
        this.isShared = isShared;

        this.sharers = sharers;
        if (null == this.sharers) {
            this.sharers = new ArrayList<Sharer>();
        }

        this.versions = versions;
        if (null == this.versions) {
            this.versions = new ArrayList<Version>();
        }
    }

    /**
     * Returns the name of the file or directory (without the path to it)
     *
     * @return The name of the file
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the path to the file (without the name)
     *
     * @return The path to the file
     */
    public String getPath() {
        return path;
    }

    /**
     * Returns the path (including the file name) to the file
     *
     * @return The path to the file
     */
    public String getAbsolutePath() {
        if (! this.getPath().isEmpty()) {
            return this.getPath() + "/" + this.getName();
        }

        return this.getName();
    }

    /**
     * Returns the path type
     *
     * @return The path type
     */
    public PathType getPathType() {
        return pathType;
    }

    /**
     * Returns true if this file is shared
     *
     * @return True, if shared, false otherwise
     */
    public boolean isShared() {
        return isShared;
    }

    /**
     * Returns a list of sharers of this file
     *
     * @return The list of sharers
     */
    public List<Sharer> getSharers() {
        return sharers;
    }

    /**
     * Returns a list of versions of this file
     *
     * @return THe list of sharers
     */
    public List<Version> getVersions() {
        return versions;
    }

    /**
     * Converts this object to a JSON representation
     *
     * @return A JSON string
     */
    public String toJson() {
        return gson.toJson(this, PathObject.class);
    }

    /**
     * Creates a PathObject from its JSON representation
     *
     * @param json The json string
     *
     * @return The path object created of it
     */
    public static PathObject fromJson(String json) {
        return gson.fromJson(json, PathObject.class);
    }
}
