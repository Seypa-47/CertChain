package com.certchain.common.exception;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ApiError(
    Instant timestamp, int status, String error, String message, String path,
    List<FieldIssue> details, String traceId
) {
    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(Instant.now(), status, error, message, path, List.of(), UUID.randomUUID().toString());
    }
    public record FieldIssue(String field, String message) {}
}
