package be.cm.todoapplication.controller;

import be.cm.todoapplication.dto.TodoDTO;
import be.cm.todoapplication.service.PdfService;
import be.cm.todoapplication.service.TodoService;
import be.cm.todoapplication.service.TodoSyncService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TodoController.class)
class TodoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TodoService todoService;

    @MockBean
    private PdfService pdfService;

    @MockBean
    private TodoSyncService todoSyncService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void shouldGetAllTodos() throws Exception {
        // Given
        TodoDTO todo = new TodoDTO();
        todo.setId(1L);
        todo.setTitle("Test Todo");
        todo.setCompleted(false);
        todo.setUsername("testuser");
        todo.setCreatedAt(LocalDateTime.now());

        Page<TodoDTO> todoPage = new PageImpl<>(Arrays.asList(todo));
        when(todoService.getUserTodos(any(), any(), any())).thenReturn(todoPage);

        // When & Then
        mockMvc.perform(get("/api/todos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Test Todo"))
                .andExpect(jsonPath("$.content[0].completed").value(false));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void shouldCreateTodo() throws Exception {
        // Given
        TodoDTO inputTodo = new TodoDTO();
        inputTodo.setTitle("New Todo");
        inputTodo.setDescription("Test Description");
        inputTodo.setCompleted(false);

        TodoDTO savedTodo = new TodoDTO();
        savedTodo.setId(1L);
        savedTodo.setTitle("New Todo");
        savedTodo.setDescription("Test Description");
        savedTodo.setCompleted(false);
        savedTodo.setUsername("testuser");
        savedTodo.setCreatedAt(LocalDateTime.now());

        when(todoService.createTodo(any(TodoDTO.class))).thenReturn(savedTodo);

        // When & Then
        mockMvc.perform(post("/api/todos")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputTodo)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("New Todo"))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void shouldGetTodoById() throws Exception {
        // Given
        TodoDTO todo = new TodoDTO();
        todo.setId(1L);
        todo.setTitle("Test Todo");
        todo.setCompleted(false);
        todo.setUsername("testuser");

        when(todoService.getTodoById(1L)).thenReturn(Optional.of(todo));

        // When & Then
        mockMvc.perform(get("/api/todos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Todo"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void shouldReturnNotFoundForNonExistentTodo() throws Exception {
        // Given
        when(todoService.getTodoById(999L)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/todos/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRequireAuthenticationForProtectedEndpoints() throws Exception {
        mockMvc.perform(get("/api/todos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void shouldAllowAdminToSync() throws Exception {
        // Given
        doNothing().when(todoService).syncFromJsonPlaceholder();

        // When & Then
        mockMvc.perform(post("/api/todos/sync").with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void shouldForbidUserFromSyncing() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/todos/sync").with(csrf()))
                .andExpect(status().isForbidden());
    }
}
