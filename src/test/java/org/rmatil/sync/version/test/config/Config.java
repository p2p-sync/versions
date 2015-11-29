package org.rmatil.sync.version.test.config;

import java.nio.file.Path;
import java.nio.file.Paths;

public enum Config {
    DEFAULT();

    private Path rootTestDir;

    Config() {
        rootTestDir = Paths.get("./org.rmatil.sync.version.test.dir");
    }

    public Path getRootTestDir() {
        return this.rootTestDir;
    }
}
