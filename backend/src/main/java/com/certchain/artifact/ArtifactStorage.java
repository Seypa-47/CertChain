package com.certchain.artifact;

public interface ArtifactStorage {
    void write(String key, byte[] content);
    byte[] read(String key);
    boolean exists(String key);
}
