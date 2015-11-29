package org.rmatil.sync.version.config;

import org.rmatil.sync.commons.hashing.HashingAlgorithm;

public enum Config {

    DEFAULT(HashingAlgorithm.SHA_256);

    private HashingAlgorithm hashingAlgorithm;

    Config(HashingAlgorithm hashingAlgorithm) {
        this.hashingAlgorithm = hashingAlgorithm;
    }

    public HashingAlgorithm getHashingAlgorithm() {
        return hashingAlgorithm;
    }
}
