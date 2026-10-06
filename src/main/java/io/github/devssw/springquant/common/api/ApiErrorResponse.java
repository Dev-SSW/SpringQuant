package io.github.devssw.springquant.common.api;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        // 어떤 입력 필드가 잘못됐는지 저장
        Map<String, String> fieldErrors
) {
    public ApiErrorResponse {
        Objects.requireNonNull(timestamp, "timestamp가 null이면 안됩니다.");
        Objects.requireNonNull(error, "error가 null이면 안됩니다.");
        Objects.requireNonNull(message, "message가 null이면 안됩니다.");
        Objects.requireNonNull(path, "path가 null이면 안됩니다.");
        Objects.requireNonNull(fieldErrors, "fieldErrors가 null이면 안됩니다.");

        fieldErrors = Map.copyOf(new LinkedHashMap<>(fieldErrors));
    }
}