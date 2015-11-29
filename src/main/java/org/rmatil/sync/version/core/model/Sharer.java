package org.rmatil.sync.version.core.model;

import org.rmatil.sync.version.api.AccessType;

public class Sharer {

    protected String address;

    protected AccessType accessType;

    public Sharer(String address, AccessType accessType) {
        this.address = address;
        this.accessType = accessType;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public AccessType getAccessType() {
        return accessType;
    }

    public void setAccessType(AccessType accessType) {
        this.accessType = accessType;
    }
}
