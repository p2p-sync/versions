package org.rmatil.sync.version.core.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.rmatil.sync.version.api.DeleteType;

import java.util.List;

public class Delete {

    protected DeleteType   deleteType;
    protected List<String> deleteHistory;

    public Delete(DeleteType deleteType, List<String> deleteHistory) {
        this.deleteType = deleteType;
        this.deleteHistory = deleteHistory;
    }

    public DeleteType getDeleteType() {
        return deleteType;
    }

    public void setDeleteType(DeleteType deleteType) {
        this.deleteType = deleteType;
    }

    public List<String> getDeleteHistory() {
        return deleteHistory;
    }

    public void setDeleteHistory(List<String> deleteHistory) {
        this.deleteHistory = deleteHistory;
    }

    @Override
    public int hashCode() {
        // http://stackoverflow.com/questions/27581/what-issues-should-be-considered-when-overriding-equals-and-hashcode-in-java
        HashCodeBuilder builder = new HashCodeBuilder(17, 31)
                .append(deleteType);

        this.deleteHistory.forEach(builder::append);

        return builder.toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        // http://stackoverflow.com/questions/27581/what-issues-should-be-considered-when-overriding-equals-and-hashcode-in-java
        if (! (obj instanceof Delete)) {
            return false;
        }
        if (obj == this) {
            return true;
        }

        Delete rhs = (Delete) obj;
        EqualsBuilder builder = new EqualsBuilder()
                .append(this.deleteType, rhs.getDeleteType());

        if (this.deleteHistory.size() == rhs.getDeleteHistory().size()) {
            for (int i = 0; i < this.deleteHistory.size(); i++) {
                builder.append(this.deleteHistory.get(i), rhs.getDeleteHistory().get(i));
            }
        } else {
            builder.append(this.deleteHistory.size(), rhs.getDeleteHistory().size());
        }

        return builder.isEquals();
    }
}
