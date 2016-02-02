package org.rmatil.sync.version.core.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.rmatil.sync.version.api.AccessType;
import org.rmatil.sync.version.api.PathType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PathObject {

    protected static Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();

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
     * The access type the client has on this file
     * if it is shared. May be null otherwise
     */
    protected AccessType accessType;

    /**
     * Whether this object is shared
     */
    protected boolean isShared;

    /**
     * Whether the corresponding file on disk is deleted
     */
    protected boolean isDeleted;

    /**
     * The username of the file owner in case the
     * file is shared.
     */
    protected String owner;

    /**
     * A list of sharers of this file
     */
    protected Set<Sharer> sharers;

    /**
     * A list of versions of this file
     */
    protected List<Version> versions;

    /**
     * @param name       The name of the file or directory (without the path to it)
     * @param path       The path to the file or directory (without the name of it)
     * @param pathType   The type of the file
     * @param accessType The access type the client has on this file if it is shared. May be null otherwise
     * @param isShared   Whether this file is shared
     * @param isDeleted  Whether the path on disk is deleted
     * @param owner      The owner's username in case the file is shared
     * @param sharers    A list of sharers
     * @param versions   A list of versions
     */
    public PathObject(String name, String path, PathType pathType, AccessType accessType, boolean isShared, boolean isDeleted, String owner, Set<Sharer> sharers, List<Version> versions) {
        this.name = name;
        this.path = path;
        this.pathType = pathType;
        this.accessType = accessType;
        this.isShared = isShared;
        this.isDeleted = isDeleted;
        this.owner = owner;
        this.sharers = sharers;
        if (null == this.sharers) {
            this.sharers = new HashSet<>();
        }

        this.versions = versions;
        if (null == this.versions) {
            this.versions = new ArrayList<>();
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
     * Returns the access type the client has on this file
     * if it is shared. May be null otherwise
     *
     * @return The access type of this file
     */
    public AccessType getAccessType() {
        return accessType;
    }

    /**
     * Sets the access type the client has on this file
     * if it is shared. May be null otherwise
     *
     * @param accessType The access type
     */
    public void setAccessType(AccessType accessType) {
        this.accessType = accessType;
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
     * Sets the flag whether the file represented by this
     * path object is share or not
     *
     * @param isShared Whether the file is shared or not
     */
    public void setIsShared(boolean isShared) {
        this.isShared = isShared;
    }

    /**
     * Returns true if the path on disk is deleted
     *
     * @return True, if deleted on disk, false otherwise
     */
    public boolean isDeleted() {
        return isDeleted;
    }

    /**
     * Returns the owner of this file
     * in case it is shared. May be null otherwise.
     *
     * @return The owner's username or null, if the file is not shared
     */
    public String getOwner() {
        return owner;
    }

    /**
     * Sets the owner's username
     *
     * @param owner The owner's username
     */
    public void setOwner(String owner) {
        this.owner = owner;
    }

    /**
     * Returns a list of sharers of this file
     *
     * @return The list of sharers
     */
    public Set<Sharer> getSharers() {
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
