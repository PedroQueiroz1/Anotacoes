package com.tasknotes.notes.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasknotes.categories.model.Category;
import com.tasknotes.notes.dto.NoteResponse;
import com.tasknotes.notes.dto.SmartFormatApplyRequest;
import com.tasknotes.notes.dto.SmartFormatPreviewResponse;
import com.tasknotes.notes.model.Note;
import com.tasknotes.notes.repository.NoteRepository;
import com.tasknotes.shared.exception.ResourceNotFoundException;
import com.tasknotes.shared.util.HtmlSanitizer;
import com.tasknotes.users.service.SecurityHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class SmartTextFormattingService {

    private static final Logger log = LoggerFactory.getLogger(SmartTextFormattingService.class);
    private static final int MAX_PLAIN_CONTENT = 2000;

    @Value("${app.smart-format.enabled:false}")
    private boolean enabled;

    @Value("${app.smart-format.mcp.url:http://localhost:8790/internal/format-note}")
    private String mcpUrl;

    @Value("${app.smart-format.timeout-ms:10000}")
    private int timeoutMs;

    private final NoteRepository noteRepository;
    private final SecurityHelper securityHelper;
    private final ObjectMapper objectMapper;
    private final NoteService noteService;

    public SmartTextFormattingService(NoteRepository noteRepository,
                                      SecurityHelper securityHelper,
                                      ObjectMapper objectMapper,
                                      NoteService noteService) {
        this.noteRepository = noteRepository;
        this.securityHelper = securityHelper;
        this.objectMapper   = objectMapper;
        this.noteService    = noteService;
    }

    // ── Preview ───────────────────────────────────────────────────────────────

    public SmartFormatPreviewResponse preview(Long noteId) {
        if (!enabled) {
            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Formatação inteligente indisponível no momento."
            );
        }

        Note note = findOrThrow(noteId);

        Map<String, Object> body = Map.of(
            "titlePlain",   note.getTitle() != null ? note.getTitle() : "",
            "contentPlain", note.getContent() != null ? note.getContent() : "",
            "contentHtml",  note.getContentHtml() != null ? note.getContentHtml() : "",
            "language",     "pt-BR",
            "formattingLevel", "balanced"
        );

        try {
            JsonNode json = callMcp(body);
            return new SmartFormatPreviewResponse(
                noteId,
                json.path("formattedTitleHtml").asText(""),
                json.path("formattedContentHtml").asText(""),
                json.path("plainTextPreserved").asBoolean(true),
                toStringList(json.path("operationsSummary")),
                toStringList(json.path("warnings"))
            );
        } catch (Exception e) {
            log.warn("smart_format_preview_error noteId={} error={}", noteId, e.getMessage());
            throw new RuntimeException("Falha ao gerar formatação inteligente. Tente novamente.");
        }
    }

    // ── Apply ─────────────────────────────────────────────────────────────────

    @Transactional
    public NoteResponse apply(Long noteId, SmartFormatApplyRequest request) {
        Note note = findOrThrow(noteId);

        String sanitizedHtml  = HtmlSanitizer.sanitize(request.formattedContentHtml() != null
                ? request.formattedContentHtml() : "");
        String plainText      = HtmlSanitizer.toPlainText(sanitizedHtml, MAX_PLAIN_CONTENT);

        note.setContentHtml(sanitizedHtml.isBlank() ? null : sanitizedHtml);
        note.setContent(plainText.isBlank() ? null : plainText);
        Note saved = noteRepository.save(note);

        log.info("smart_format_applied noteId={}", noteId);
        return noteService.toResponse(
                noteRepository.findByIdWithTags(saved.getId()).orElse(saved));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Note findOrThrow(Long noteId) {
        Long ownerId = securityHelper.currentUserId();
        Note note = noteRepository.findByIdWithCategoryAndOwner(noteId)
                .orElseThrow(() -> new ResourceNotFoundException("Anotação não encontrada: " + noteId));
        Category cat = note.getCategory();
        if (cat.getOwner() == null || !ownerId.equals(cat.getOwner().getId())) {
            throw new ResourceNotFoundException("Anotação não encontrada: " + noteId);
        }
        return note;
    }

    private JsonNode callMcp(Map<String, Object> body) throws Exception {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        RestTemplate restTemplate = new RestTemplate(factory);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(mcpUrl, entity, String.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("MCP respondeu com status " + response.getStatusCode());
        }
        return objectMapper.readTree(response.getBody());
    }

    private List<String> toStringList(JsonNode node) {
        if (!node.isArray()) return List.of();
        List<String> result = new java.util.ArrayList<>();
        for (JsonNode item : node) result.add(item.asText());
        return result;
    }
}
