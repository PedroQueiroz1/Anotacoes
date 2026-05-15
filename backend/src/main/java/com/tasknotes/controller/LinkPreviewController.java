package com.tasknotes.controller;

import com.tasknotes.dto.LinkPreviewResponse;
import com.tasknotes.service.LinkPreviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Link Preview", description = "Preview de links do YouTube")
public class LinkPreviewController {

    private final LinkPreviewService service;

    public LinkPreviewController(LinkPreviewService service) {
        this.service = service;
    }

    @GetMapping("/api/link-preview/youtube")
    @Operation(summary = "Busca o título público de um vídeo do YouTube via oEmbed")
    public LinkPreviewResponse youtube(@RequestParam String url) {
        return service.fetchYoutube(url);
    }
}
