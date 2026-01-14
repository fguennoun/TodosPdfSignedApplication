package be.cm.todoapplication.controller;

import be.cm.todoapplication.dto.UserDTO;
import be.cm.todoapplication.model.Todo;
import be.cm.todoapplication.service.PdfService;
import be.cm.todoapplication.service.TodoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/todos")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class TodoController {

    private final TodoService todoService;
    private final PdfService pdfService;

    @PostMapping("/sync")
    public ResponseEntity<String> syncTodos() {
        todoService.syncFromJsonPlaceholder();
        return ResponseEntity.ok("Todos synchronized successfully");
    }

    @GetMapping
    public ResponseEntity<List<Todo>> getAllTodos() {
        return ResponseEntity.ok(todoService.getAllTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Todo> getTodoById(@PathVariable Long id) {
        return todoService.getTodoById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Todo> createTodo(@RequestBody Todo todo) {
        return ResponseEntity.ok(todoService.createTodo(todo));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Todo> updateTodo(@PathVariable Long id, @RequestBody Todo todo) {
        return ResponseEntity.ok(todoService.updateTodo(id, todo));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTodo(@PathVariable Long id) {
        todoService.deleteTodo(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(todoService.getAllUsers());
    }

    @GetMapping("/users/pdf")
    public ResponseEntity<byte[]> getUsersListPdf() {
        try {
            byte[] pdf = pdfService.generateUsersListPdf(todoService.getAllUsers());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(
                    ContentDisposition.builder("attachment")
                            .filename("liste-utilisateurs.pdf")
                            .build()
            );

            return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> getTodoPdf(@PathVariable Long id) {
        try {
            Todo todo = todoService.getTodoById(id)
                    .orElseThrow(() -> new RuntimeException("Todo not found"));

            byte[] pdf = pdfService.generateTodoPdf(todo);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(
                    ContentDisposition.builder("attachment")
                            .filename("tache-" + id + ".pdf")
                            .build()
            );

            return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{id}/pdf/sign")
    public ResponseEntity<byte[]> getTodoPdfWithSignature(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        try {
            Todo todo = todoService.getTodoById(id)
                    .orElseThrow(() -> new RuntimeException("Todo not found"));

            String signature = payload.get("signature");
            byte[] pdf = pdfService.generateTodoPdfWithSignature(todo, signature);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(
                    ContentDisposition.builder("attachment")
                            .filename("tache-signee-" + id + ".pdf")
                            .build()
            );

            return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
