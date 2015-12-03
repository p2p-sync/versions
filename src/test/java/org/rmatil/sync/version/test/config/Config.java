package org.rmatil.sync.version.test.config;

import org.rmatil.sync.commons.hashing.HashingAlgorithm;

import java.nio.file.Path;
import java.nio.file.Paths;

public enum Config {
    DEFAULT();

    private Path rootTestDir;

    private HashingAlgorithm hashingAlgorithm;

    Config() {
        rootTestDir = Paths.get("./org.rmatil.sync.version.test.dir");
        hashingAlgorithm = HashingAlgorithm.SHA_256;
    }

    public Path getRootTestDir() {
        return this.rootTestDir;
    }

    public HashingAlgorithm getHashingAlgorithm() {
        return this.hashingAlgorithm;
    }
}
