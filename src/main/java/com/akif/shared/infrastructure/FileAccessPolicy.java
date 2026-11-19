package com.akif.shared.infrastructure;

public interface FileAccessPolicy {

    String directory();

    boolean isOwnedBy(String filename, Long userId);
}
