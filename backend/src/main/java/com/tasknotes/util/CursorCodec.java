package com.tasknotes.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Base64;

public final class CursorCodec {

    private static final Logger log = LoggerFactory.getLogger(CursorCodec.class);
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private CursorCodec() {}

    public record CursorPayload(
            @JsonProperty("c") LocalDateTime createdAt,
            @JsonProperty("i") Long id
    ) {}

    public static String encode(LocalDateTime createdAt, Long id) {
        try {
            byte[] json = MAPPER.writeValueAsBytes(new CursorPayload(createdAt, id));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json);
        } catch (Exception e) {
            throw new IllegalStateException("cursor_encode_failed", e);
        }
    }

    public static CursorPayload decode(String cursor) {
        if (cursor == null || cursor.isBlank()) return null;
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(cursor);
            return MAPPER.readValue(bytes, CursorPayload.class);
        } catch (Exception e) {
            log.warn("pagination_cursor_invalid cursorPresent=true reason=decode_failed");
            return null;
        }
    }
}
