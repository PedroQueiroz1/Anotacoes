package com.tasknotes.shared.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Base64;

public final class NoteCursorCodec {

    private static final Logger log = LoggerFactory.getLogger(NoteCursorCodec.class);
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private NoteCursorCodec() {}

    // zone 0 = positioned notes (sorted by position ASC)
    // zone 1 = unpositioned notes (sorted by created_at DESC)
    public record Payload(
            @JsonProperty("z") int zone,
            @JsonProperty("p") Integer position,
            @JsonProperty("c") LocalDateTime createdAt,
            @JsonProperty("i") Long id
    ) {}

    public static String encodePositioned(int position, Long id) {
        try {
            byte[] json = MAPPER.writeValueAsBytes(new Payload(0, position, null, id));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json);
        } catch (Exception e) {
            throw new IllegalStateException("note_cursor_encode_failed", e);
        }
    }

    public static String encodeUnpositioned(LocalDateTime createdAt, Long id) {
        try {
            byte[] json = MAPPER.writeValueAsBytes(new Payload(1, null, createdAt, id));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json);
        } catch (Exception e) {
            throw new IllegalStateException("note_cursor_encode_failed", e);
        }
    }

    public static Payload decode(String cursor) {
        if (cursor == null || cursor.isBlank()) return null;
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(cursor);
            Payload p = MAPPER.readValue(bytes, Payload.class);
            // Backward compat: old cursors have no "z" field → default to zone 1
            return p;
        } catch (Exception e) {
            log.warn("note_pagination_cursor_invalid reason=decode_failed");
            return null;
        }
    }
}
