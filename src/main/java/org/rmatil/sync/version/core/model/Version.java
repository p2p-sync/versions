package org.rmatil.sync.version.core.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * The version of a certain PathObject.
 * This class is immutable to guarantee equality
 * while comparing two versions where one
 * was read from a third party source (e.g. external storage adapter).
 *
 * @see org.rmatil.sync.version.core.VersionManager#removeVersion(Version, String) Used in VersionManager to remove the version from the list of stored ones
 */
public final class Version {

    private final String hash;

    public Version(String hash) {
        this.hash = hash;
    }

    public String getHash() {
        return hash;
    }

    @Override
    public int hashCode() {
        // http://stackoverflow.com/questions/27581/what-issues-should-be-considered-when-overriding-equals-and-hashcode-in-java
        return new HashCodeBuilder(17, 31).append(hash).toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        // http://stackoverflow.com/questions/27581/what-issues-should-be-considered-when-overriding-equals-and-hashcode-in-java
        if (! (obj instanceof Version)) {
            return false;
        }
        if (obj == this) {
            return true;
        }

        Version rhs = (Version) obj;
        return new EqualsBuilder().append(hash, rhs.getHash()).isEquals();
    }
}
