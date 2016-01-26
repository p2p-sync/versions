package org.rmatil.sync.version.core.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.rmatil.sync.version.api.AccessType;

public class Sharer {

    protected String username;

    protected AccessType accessType;

    public Sharer(String username, AccessType accessType) {
        this.username = username;
        this.accessType = accessType;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public AccessType getAccessType() {
        return accessType;
    }

    public void setAccessType(AccessType accessType) {
        this.accessType = accessType;
    }


    @Override
    public int hashCode() {
        // http://stackoverflow.com/questions/27581/what-issues-should-be-considered-when-overriding-equals-and-hashcode-in-java
        return new HashCodeBuilder(17, 31)
                .append(username)
                .append(accessType)
                .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        // http://stackoverflow.com/questions/27581/what-issues-should-be-considered-when-overriding-equals-and-hashcode-in-java
        if (! (obj instanceof Sharer)) {
            return false;
        }
        if (obj == this) {
            return true;
        }

        Sharer rhs = (Sharer) obj;
        return new EqualsBuilder()
                .append(username, rhs.getUsername())
                .append(accessType, rhs.getAccessType())
                .isEquals();
    }
}
