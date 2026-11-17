package com.example.zolieapi.common;

import com.example.zolieapi.exception.ApiException;
import com.example.zolieapi.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

public record Cursor(Instant time, UUID id) {

    private static final Cursor START = new Cursor(
            Instant.parse("9999-01-01T00:00:00Z"), UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"));

    private static final Cursor OLDEST = new Cursor(Instant.EPOCH, new UUID(0L, 0L));

    public static Cursor decodeAscending(String encoded) {
        return encoded == null || encoded.isBlank() ? OLDEST : decode(encoded);
    }

    public static Cursor ofId(UUID id) {
        return new Cursor(Instant.EPOCH, id);
    }

    public static Cursor decode(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return START;
        }
        try {
            String[] parts = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8).split("_", 2);
            long micros = Long.parseLong(parts[0]);
            Instant time = Instant.EPOCH.plus(micros, ChronoUnit.MICROS);
            return new Cursor(time, UUID.fromString(parts[1]));
        } catch (RuntimeException e) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "cursor is not valid");
        }
    }

    public String encode() {
        long micros = ChronoUnit.MICROS.between(Instant.EPOCH, time);
        String raw = micros + "_" + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }
}
