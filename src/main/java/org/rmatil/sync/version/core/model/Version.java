package org.rmatil.sync.version.core.model;

public class Version {

    protected String hash;

    public Version(String hash) {
        this.hash = hash;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }
}
