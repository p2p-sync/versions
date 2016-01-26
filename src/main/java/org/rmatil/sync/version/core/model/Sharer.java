package org.rmatil.sync.version.core.model;

import org.rmatil.sync.version.api.AccessType;

public class Sharer {

    protected String address;

    protected int port;

    protected AccessType accessType;

    public Sharer(String address, int port, AccessType accessType) {
        this.address = address;
        this.port = port;
        this.accessType = accessType;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public AccessType getAccessType() {
        return accessType;
    }

    public void setAccessType(AccessType accessType) {
        this.accessType = accessType;
    }
}
