package com.tasknotes.service;

import com.tasknotes.model.Category;
import com.tasknotes.model.Note;
import com.tasknotes.model.Subtask;
import com.tasknotes.model.Task;
import com.tasknotes.repository.CategoryRepository;
import com.tasknotes.repository.NoteRepository;
import com.tasknotes.repository.SubtaskRepository;
import com.tasknotes.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class CategoryExportService {

    private static final Logger log = LoggerFactory.getLogger(CategoryExportService.class);

    private static final String LINE  = "=".repeat(50);
    private static final String SEP   = "-".repeat(50);
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter D_FMT  = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final CategoryRepository categoryRepository;
    private final TaskRepository     taskRepository;
    private final NoteRepository     noteRepository;
    private final SubtaskRepository  subtaskRepository;

    public CategoryExportService(CategoryRepository categoryRepository,
                                  TaskRepository taskRepository,
                                  NoteRepository noteRepository,
                                  SubtaskRepository subtaskRepository) {
        this.categoryRepository = categoryRepository;
        this.taskRepository     = taskRepository;
        this.noteRepository     = noteRepository;
        this.subtaskRepository  = subtaskRepository;
    }

    public String buildTxt(String slug, String correlationId) {
        log.info("category_export_txt_started categorySlug={} correlationId={}", slug, correlationId);
        long start = System.currentTimeMillis();

        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> {
                    log.warn("category_export_txt_not_found categoryIdentifier={} correlationId={}", slug, correlationId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoria não encontrada.");
                });

        List<Task> tasks = taskRepository.findByCategoryIdOrdered(category.getId());
        List<Note> notes = noteRepository.findByCategoryIdOrdered(category.getId());

        int subtaskCount = 0;
        StringBuilder sb = new StringBuilder(4096);

        // Header
        sb.append("TASKNOTES — EXPORTAÇÃO DE CATEGORIA\n");
        sb.append(LINE).append("\n\n");
        sb.append("Categoria: ").append(category.getName()).append("\n");
        if (category.getSlug() != null) {
            sb.append("Slug: ").append(category.getSlug()).append("\n");
        }
        sb.append("Exportado em: ").append(format(LocalDateTime.now())).append("\n");
        sb.append("Total de tarefas: ").append(tasks.size()).append("\n");
        sb.append("Total de anotações: ").append(notes.size()).append("\n\n");

        // Tasks section
        sb.append(LINE).append("\n");
        sb.append("TAREFAS\n");
        sb.append(LINE).append("\n");

        if (tasks.isEmpty()) {
            sb.append("\nSem tarefas.\n");
        } else {
            for (int i = 0; i < tasks.size(); i++) {
                Task task = tasks.get(i);
                sb.append("\n[").append(i + 1).append("] ").append(task.getTitle()).append("\n");
                sb.append("Status: ").append(statusLabel(task.getStatus().name())).append("\n");
                sb.append("Prioridade: ").append(priorityLabel(task.getPriority().name())).append("\n");
                sb.append("Prazo: ").append(task.getDueDate() != null ? format(task.getDueDate()) : "Sem prazo").append("\n");
                if (task.getCreatedAt() != null) {
                    sb.append("Criada em: ").append(format(task.getCreatedAt())).append("\n");
                }

                sb.append("\nDescrição:\n");
                sb.append(task.getDescription() != null && !task.getDescription().isBlank()
                        ? task.getDescription() : "Sem descrição.").append("\n");

                List<Subtask> subtasks = subtaskRepository.findByTaskIdOrderByCreatedAtAsc(task.getId());
                subtaskCount += subtasks.size();

                sb.append("\nChecklist:\n");
                if (subtasks.isEmpty()) {
                    sb.append("Sem subtarefas.\n");
                } else {
                    for (Subtask sub : subtasks) {
                        sb.append(sub.isDone() ? "- [x] " : "- [ ] ").append(sub.getText()).append("\n");
                    }
                }

                if (i < tasks.size() - 1) {
                    sb.append("\n").append(SEP).append("\n");
                }
            }
        }

        // Notes section
        sb.append("\n").append(LINE).append("\n");
        sb.append("ANOTAÇÕES\n");
        sb.append(LINE).append("\n");

        if (notes.isEmpty()) {
            sb.append("\nSem anotações.\n");
        } else {
            for (int i = 0; i < notes.size(); i++) {
                Note note = notes.get(i);
                sb.append("\n[").append(i + 1).append("] ").append(note.getTitle()).append("\n");
                if (note.getCreatedAt() != null) {
                    sb.append("Criada em: ").append(format(note.getCreatedAt())).append("\n");
                }

                sb.append("\nConteúdo:\n");
                sb.append(note.getContent() != null && !note.getContent().isBlank()
                        ? note.getContent() : "(sem conteúdo)").append("\n");

                if (i < notes.size() - 1) {
                    sb.append("\n").append(SEP).append("\n");
                }
            }
        }

        String content = sb.toString();
        long duration = System.currentTimeMillis() - start;
        int fileSizeBytes = content.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;

        log.info("category_export_txt_completed categorySlug={} taskCount={} noteCount={} subtaskCount={} fileSizeBytes={} durationMs={} correlationId={}",
                slug, tasks.size(), notes.size(), subtaskCount, fileSizeBytes, duration, correlationId);

        return content;
    }

    private String statusLabel(String status) {
        return switch (status) {
            case "TODO"        -> "A fazer";
            case "IN_PROGRESS" -> "Em andamento";
            case "DONE"        -> "Concluída";
            default            -> status;
        };
    }

    private String priorityLabel(String priority) {
        return switch (priority) {
            case "LOW"    -> "Baixa";
            case "MEDIUM" -> "Média";
            case "HIGH"   -> "Alta";
            default       -> priority;
        };
    }

    private String format(LocalDateTime dt) {
        return dt.format(DT_FMT);
    }

    private String format(LocalDate d) {
        return d.format(D_FMT);
    }
}
