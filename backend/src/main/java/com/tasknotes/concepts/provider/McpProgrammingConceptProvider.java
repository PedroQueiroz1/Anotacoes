package com.tasknotes.concepts.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasknotes.concepts.dto.ConceptSuggestionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

@Component
@Order(2)
public class McpProgrammingConceptProvider implements ProgrammingConceptProvider {

    private static final Logger log = LoggerFactory.getLogger(McpProgrammingConceptProvider.class);

    @Value("${app.concepts.mcp.enabled:false}")
    private boolean enabled;

    @Value("${app.concepts.mcp.url:http://localhost:8787/internal/concepts/explain}")
    private String mcpUrl;

    @Value("${app.concepts.mcp.timeout-ms:5000}")
    private int timeoutMs;

    @Value("${app.concepts.mcp.internet-fallback-enabled:false}")
    private boolean internetFallback;

    private final ObjectMapper objectMapper;

    public McpProgrammingConceptProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean isEnabled() { return enabled; }

    @Override
    public Optional<ConceptSuggestionResponse> explain(String normalizedTerm, ConceptLookupContext context) {
        try {
            RestTemplate restTemplate = buildRestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = Map.of(
                "term",                normalizedTerm,
                "language",            "pt-BR",
                "maxLength",           900,
                "useInternetFallback", internetFallback
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(mcpUrl, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return Optional.empty();
            }

            JsonNode json = objectMapper.readTree(response.getBody());

            if (!json.path("found").asBoolean(false)) {
                log.debug("concept_mcp_not_found termNormalized={}", normalizedTerm);
                return Optional.empty();
            }

            String summary = json.path("summary").asText(null);
            if (summary == null || summary.isBlank()) return Optional.empty();

            String termLabel = json.path("term").asText(normalizedTerm);
            String sourceType = resolveSourceType(json);

            log.info("concept_mcp_hit termNormalized={} sourceType={} confidence={}",
                    normalizedTerm, sourceType, json.path("confidence").asDouble(0));

            return Optional.of(new ConceptSuggestionResponse(
                    true,
                    "MCP:" + sourceType,
                    new ConceptSuggestionResponse.ConceptItem(null, termLabel, summary)
            ));

        } catch (Exception e) {
            log.warn("concept_mcp_error termNormalized={} error={}", normalizedTerm, e.getMessage());
            return Optional.empty();
        }
    }

    private RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        return new RestTemplate(factory);
    }

    private String resolveSourceType(JsonNode json) {
        JsonNode sources = json.path("sources");
        if (sources.isArray() && sources.size() > 0) {
            return sources.get(0).path("type").asText("mcp");
        }
        return json.path("sourceType").asText("mcp");
    }
}
