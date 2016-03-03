package org.rmatil.sync.version.test.core.model;

import org.junit.BeforeClass;
import org.junit.Test;
import org.rmatil.sync.version.api.DeleteType;
import org.rmatil.sync.version.core.model.Delete;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

public class DeleteTest {

    private static Delete DELETE;

    @BeforeClass
    public static void setUp() {
        DELETE = new Delete(
                DeleteType.DELETED,
                new ArrayList<>()
        );
    }

    @Test
    public void testAccessor() {
        assertEquals("delete type should be deleted", DeleteType.DELETED, DELETE.getDeleteType());
        assertEquals("delete history should be equals", 0, DELETE.getDeleteHistory().size());
    }
}
