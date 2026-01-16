package be.cm.todoapplication.controller;

import be.cm.todoapplication.dto.TodoDTO;
import be.cm.todoapplication.service.PdfService;
import be.cm.todoapplication.service.TodoService;
import be.cm.todoapplication.service.TodoSyncService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/todos")
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor
@Slf4j
public class TodoController {

    private final TodoService todoService;
    private final PdfService pdfService;
    private final TodoSyncService todoSyncService;

    /**
     * Synchronisation avec JSONPlaceholder - Admin uniquement
     */
    @PostMapping("/sync")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> syncTodos(Principal principal) {
        try {
            log.info("Synchronisation demandée par: {}", principal.getName());
            todoService.syncFromJsonPlaceholder();
            return ResponseEntity.ok("Synchronisation terminée avec succès");
        } catch (Exception e) {
            log.error("Erreur lors de la synchronisation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur de synchronisation: " + e.getMessage());
        }
    }

    /**
     * Récupère les todos de l'utilisateur avec pagination
     */
    @GetMapping
    public ResponseEntity<Page<TodoDTO>> getAllTodos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) Boolean completed,
            @RequestParam(required = false) String search,
            Principal principal) {

        try {
            Sort sort = sortDir.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

            Pageable pageable = PageRequest.of(page, Math.min(size, 100), sort);
            Page<TodoDTO> todos = todoService.getUserTodos(pageable, completed, search);

            log.debug("Récupération de {} todos pour {}", todos.getTotalElements(), principal.getName());
            return ResponseEntity.ok(todos);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des todos: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Récupère un todo par ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<TodoDTO> getTodoById(@PathVariable Long id, Principal principal) {
        try {
            return todoService.getTodoById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Erreur lors de la récupération du todo {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Crée un nouveau todo
     */
    @PostMapping
    public ResponseEntity<?> createTodo(@Valid @RequestBody TodoDTO todoDTO, Principal principal) {
        try {
            TodoDTO createdTodo = todoService.createTodo(todoDTO);
            log.info("Todo créé: {} par {}", createdTodo.getId(), principal.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdTodo);
        } catch (Exception e) {
            log.error("Erreur lors de la création du todo: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Erreur de création: " + e.getMessage());
        }
    }

    /**
     * Met à jour un todo existant
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTodo(@PathVariable Long id,
                                       @Valid @RequestBody TodoDTO todoDTO,
                                       Principal principal) {
        try {
            TodoDTO updatedTodo = todoService.updateTodo(id, todoDTO);
            log.info("Todo {} mis à jour par {}", id, principal.getName());
            return ResponseEntity.ok(updatedTodo);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("non trouvé")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body("Erreur de mise à jour: " + e.getMessage());
        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour du todo {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Supprime un todo
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTodo(@PathVariable Long id, Principal principal) {
        try {
            todoService.deleteTodo(id);
            log.info("Todo {} supprimé par {}", id, principal.getName());
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            if (e.getMessage().contains("non trouvé")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body("Erreur de suppression: " + e.getMessage());
        } catch (Exception e) {
            log.error("Erreur lors de la suppression du todo {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Statistiques utilisateur
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getUserStats(Principal principal) {
        try {
            Map<String, Object> stats = todoService.getUserStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des statistiques: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Génère un PDF pour un todo spécifique
     */
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> getTodoPdf(@PathVariable Long id, Principal principal) {
        try {
            TodoDTO todoDTO = todoService.getTodoById(id)
                    .orElseThrow(() -> new RuntimeException("Todo non trouvé"));

            // Conversion temporaire en Todo pour le service PDF (à refactorer)
            byte[] pdf = pdfService.generateTodoPdfFromDTO(todoDTO);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(
                    ContentDisposition.builder("attachment")
                            .filename("tache-" + id + ".pdf")
                            .build()
            );

            log.info("PDF généré pour le todo {} par {}", id, principal.getName());
            return new ResponseEntity<>(pdf, headers, HttpStatus.OK);

        } catch (RuntimeException e) {
            if (e.getMessage().contains("non trouvé")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Erreur lors de la génération du PDF pour le todo {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Génère un PDF signé pour un todo
     */
    @PostMapping("/{id}/pdf/sign")
    public ResponseEntity<byte[]> getTodoPdfWithSignature(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload,
            Principal principal) {
        try {
            TodoDTO todoDTO = todoService.getTodoById(id)
                    .orElseThrow(() -> new RuntimeException("Todo non trouvé"));

            String signature = payload.get("signature");
            if (signature == null || signature.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            // Conversion temporaire en Todo pour le service PDF (à refactorer)
            byte[] pdf = pdfService.generateTodoPdfWithSignatureFromDTO(todoDTO, signature);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(
                    ContentDisposition.builder("attachment")
                            .filename("tache-signee-" + id + ".pdf")
                            .build()
            );

            log.info("PDF signé généré pour le todo {} par {}", id, principal.getName());
            return new ResponseEntity<>(pdf, headers, HttpStatus.OK);

        } catch (RuntimeException e) {
            if (e.getMessage().contains("non trouvé")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Erreur lors de la génération du PDF signé pour le todo {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Synchronisation asynchrone avec JSONPlaceholder - Admin uniquement
     */
    @PostMapping("/sync-async")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> syncTodosAsync(Principal principal) {
        try {
            log.info("Synchronisation asynchrone demandée par: {}", principal.getName());

            // Démarrer la synchronisation asynchrone
            CompletableFuture<String> syncResult = todoService.syncFromJsonPlaceholderAsync(principal.getName());

            return ResponseEntity.accepted()
                    .body(Map.of(
                        "message", "Synchronisation asynchrone démarrée",
                        "status", "ACCEPTED"
                    ));
        } catch (Exception e) {
            log.error("Erreur lors du démarrage de la synchronisation asynchrone: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur de synchronisation: " + e.getMessage());
        }
    }

    /**
     * Génération de PDF en lot de manière asynchrone
     */
    @PostMapping("/generate-bulk-pdf")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> generateBulkPdfAsync(Principal principal) {
        try {
            log.info("Génération de PDF en lot demandée par: {}", principal.getName());

            // Récupérer tous les todos de l'utilisateur
            List<TodoDTO> userTodos = todoService.getAllTodosByUser(principal.getName());

            if (userTodos.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Aucun todo trouvé pour générer le PDF"));
            }

            // Démarrer la génération asynchrone
            CompletableFuture<String> pdfResult = pdfService.processLargePdfAsync(
                principal.getName(),
                "bulk",
                userTodos
            );

            return ResponseEntity.accepted()
                    .body(Map.of(
                        "message", String.format("Génération PDF démarrée pour %d todos", userTodos.size()),
                        "status", "ACCEPTED",
                        "todoCount", userTodos.size()
                    ));

        } catch (Exception e) {
            log.error("Erreur lors du démarrage de la génération PDF en lot: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur de génération PDF: " + e.getMessage()));
        }
    }

    /**
     * Récupérer le statut d'une tâche asynchrone
     */
    @GetMapping("/task-status/{taskId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> getTaskStatus(@PathVariable String taskId, Principal principal) {
        try {
            log.info("Statut de tâche demandé par {} pour tâche: {}", principal.getName(), taskId);

            // Dans une implémentation complète, on récupérerait le statut depuis un cache Redis
            // Pour l'instant, on retourne un statut simulé
            return ResponseEntity.ok(Map.of(
                "taskId", taskId,
                "status", "IN_PROGRESS",
                "message", "Task is being processed",
                "timestamp", java.time.LocalDateTime.now()
            ));

        } catch (Exception e) {
            log.error("Erreur lors de la récupération du statut de tâche {}: {}", taskId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur de récupération du statut"));
        }
    }

    /**
     * Endpoint pour tester les notifications WebSocket
     */
    @PostMapping("/test-notification")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> testNotification(Principal principal) {
        try {
            log.info("Test de notification demandé par: {}", principal.getName());

            // Envoyer une notification de test via WebSocket
            todoService.sendTestNotification(principal.getName());

            return ResponseEntity.ok(Map.of(
                "message", "Notification de test envoyée",
                "recipient", principal.getName()
            ));

        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de la notification de test: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur d'envoi de notification"));
        }
    }
}
