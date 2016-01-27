package org.rmatil.sync.version.core.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.rmatil.sync.version.api.AccessType;

import java.util.List;

public class Sharer {

    protected String username;

    protected AccessType accessType;

    protected List<String> sharingHistory;

    public Sharer(String username, AccessType accessType, List<String> sharingHistory) {
        this.username = username;
        this.accessType = accessType;
        this.sharingHistory = sharingHistory;
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

    public List<String> getSharingHistory() {
        return sharingHistory;
    }

    public void setSharingHistory(List<String> sharingHistory) {
        this.sharingHistory = sharingHistory;
    }

    @Override
    public int hashCode() {
        // http://stackoverflow.com/questions/27581/what-issues-should-be-considered-when-overriding-equals-and-hashcode-in-java
        HashCodeBuilder builder = new HashCodeBuilder(17, 31)
                .append(username)
                .append(accessType);

        this.sharingHistory.forEach(builder::append);

        return builder.toHashCode();
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
        EqualsBuilder builder = new EqualsBuilder()
                .append(username, rhs.getUsername())
                .append(accessType, rhs.getAccessType());

        if (this.sharingHistory.size() == rhs.getSharingHistory().size()) {
            for (int i = 0; i < this.sharingHistory.size(); i++) {
                builder.append(this.sharingHistory.get(i), rhs.getSharingHistory().get(i));
            }
        } else {
            builder.append(this.sharingHistory.size(), rhs.getSharingHistory().size());
        }

        return builder.isEquals();
    }
}
