package com.tasknotes.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasknotes.dto.LinkPreviewResponse;
import com.tasknotes.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;

@Service
public class LinkPreviewService {

    private static final Set<String> YOUTUBE_HOSTS = Set.of(
            "www.youtube.com", "youtube.com", "youtu.be", "www.youtu.be"
    );

    private static final String OEMBED_BASE = "https://www.youtube.com/oembed?format=json&url=";

    private final ObjectMapper objectMapper = new ObjectMapper();

    public LinkPreviewResponse fetchYoutube(String url) {
        if (!isYoutubeUrl(url)) {
            throw new BusinessException("URL inválida: domínio não é do YouTube.");
        }
        String title = fetchTitle(url);
        return new LinkPreviewResponse(url, title);
    }

    private boolean isYoutubeUrl(String url) {
        try {
            String host = URI.create(url).getHost();
            return host != null && YOUTUBE_HOSTS.contains(host.toLowerCase());
        } catch (Exception e) {
            return false;
        }
    }

    private String fetchTitle(String url) {
        try {
            String oembedUrl = OEMBED_BASE + URLEncoder.encode(url, StandardCharsets.UTF_8);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(oembedUrl))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode node = objectMapper.readTree(response.body());
                JsonNode titleNode = node.get("title");
                if (titleNode != null && !titleNode.isNull()) {
                    return titleNode.asText();
                }
            }
        } catch (Exception ignored) {
            // silent fallback — não deve bloquear a aplicação
        }
        return null;
    }
}
