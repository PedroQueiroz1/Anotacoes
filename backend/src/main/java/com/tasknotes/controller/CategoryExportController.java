package com.tasknotes.controller;

import com.tasknotes.service.CategoryExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "Exportação de Categoria", description = "Exportar categoria como arquivo TXT")
public class CategoryExportController {

    private static final Logger log = LoggerFactory.getLogger(CategoryExportController.class);

    private final CategoryExportService exportService;

    public CategoryExportController(CategoryExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping("/{slug}/export/txt")
    @Operation(summary = "Exportar categoria como arquivo TXT")
    public ResponseEntity<byte[]> exportTxt(@PathVariable String slug) {
        String correlationId = UUID.randomUUID().toString().substring(0, 8);
        try {
            String content = exportService.buildTxt(slug, correlationId);
            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

            String safeSlug = slug.replaceAll("[^a-zA-Z0-9\\-]", "-");
            String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String filename = "tasknotes-categoria-" + safeSlug + "-" + date + ".txt";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("text/plain;charset=UTF-8"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(bytes);
        } catch (org.springframework.web.server.ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("category_export_txt_failed categoryIdentifier={} reason={} correlationId={}",
                    slug, ex.getMessage(), correlationId, ex);
            throw ex;
        }
    }
}
