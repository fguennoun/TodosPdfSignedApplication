package be.cm.todoapplication.service;

import be.cm.todoapplication.dto.TodoDTO;
import be.cm.todoapplication.model.Todo;
import be.cm.todoapplication.model.User;
import be.cm.todoapplication.repository.TodoRepository;
import be.cm.todoapplication.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TodoServiceTest {

    @Mock
    private TodoRepository todoRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TodoSyncService todoSyncService;

    @InjectMocks
    private TodoService todoService;

    private User testUser;
    private Todo testTodo;
    private TodoDTO testTodoDTO;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("password")
                .role(User.Role.USER)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build();

        testTodo = Todo.builder()
                .id(1L)
                .title("Test Todo")
                .description("Test Description")
                .completed(false)
                .user(testUser)
                .createdAt(LocalDateTime.now())
                .createdBy("testuser")
                .build();

        testTodoDTO = new TodoDTO();
        testTodoDTO.setId(1L);
        testTodoDTO.setTitle("Test Todo");
        testTodoDTO.setDescription("Test Description");
        testTodoDTO.setCompleted(false);
        testTodoDTO.setUserId(1L);
        testTodoDTO.setUsername("testuser");
    }

    @Test
    void shouldCreateTodo() {
        // Given
        mockSecurityContext();
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(todoRepository.save(any(Todo.class))).thenReturn(testTodo);

        TodoDTO inputDTO = new TodoDTO();
        inputDTO.setTitle("New Todo");
        inputDTO.setDescription("New Description");
        inputDTO.setCompleted(false);

        // When
        TodoDTO result = todoService.createTodo(inputDTO);

        // Then
        assertNotNull(result);
        assertEquals("Test Todo", result.getTitle());
        assertEquals("Test Description", result.getDescription());
        assertEquals(false, result.getCompleted());
        assertEquals("testuser", result.getUsername());

        verify(todoRepository).save(any(Todo.class));
    }

    @Test
    void shouldGetUserTodos() {
        // Given
        mockSecurityContext();
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        Page<Todo> todoPage = new PageImpl<>(Arrays.asList(testTodo));
        when(todoRepository.findByUserWithOptionalCompleted(eq(testUser), eq(null), any(Pageable.class)))
                .thenReturn(todoPage);

        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<TodoDTO> result = todoService.getUserTodos(pageable, null, null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Test Todo", result.getContent().get(0).getTitle());

        verify(todoRepository).findByUserWithOptionalCompleted(eq(testUser), eq(null), any(Pageable.class));
    }

    @Test
    void shouldGetTodoById() {
        // Given
        mockSecurityContext();
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(todoRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.of(testTodo));

        // When
        Optional<TodoDTO> result = todoService.getTodoById(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals("Test Todo", result.get().getTitle());
        assertEquals("testuser", result.get().getUsername());

        verify(todoRepository).findByIdAndUser(1L, testUser);
    }

    @Test
    void shouldUpdateTodo() {
        // Given
        mockSecurityContext();
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(todoRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.of(testTodo));
        when(todoRepository.save(any(Todo.class))).thenReturn(testTodo);

        TodoDTO updateDTO = new TodoDTO();
        updateDTO.setTitle("Updated Todo");
        updateDTO.setDescription("Updated Description");
        updateDTO.setCompleted(true);

        // When
        TodoDTO result = todoService.updateTodo(1L, updateDTO);

        // Then
        assertNotNull(result);
        verify(todoRepository).findByIdAndUser(1L, testUser);
        verify(todoRepository).save(any(Todo.class));
    }

    @Test
    void shouldDeleteTodo() {
        // Given
        mockSecurityContext();
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(todoRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.of(testTodo));

        // When
        assertDoesNotThrow(() -> todoService.deleteTodo(1L));

        // Then
        verify(todoRepository).findByIdAndUser(1L, testUser);
        verify(todoRepository).delete(testTodo);
    }

    @Test
    void shouldThrowExceptionWhenTodoNotFound() {
        // Given
        mockSecurityContext();
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(todoRepository.findByIdAndUser(999L, testUser)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> todoService.getTodoById(999L));
        assertThrows(RuntimeException.class, () -> todoService.updateTodo(999L, testTodoDTO));
        assertThrows(RuntimeException.class, () -> todoService.deleteTodo(999L));
    }

    @Test
    void shouldGetUserStats() {
        // Given
        mockSecurityContext();
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(todoRepository.countByUserAndCompleted(testUser, null)).thenReturn(10L);
        when(todoRepository.countByUserAndCompleted(testUser, true)).thenReturn(7L);
        when(todoRepository.countByUserAndCompleted(testUser, false)).thenReturn(3L);

        // When
        var stats = todoService.getUserStats();

        // Then
        assertEquals(10L, stats.get("totalTodos"));
        assertEquals(7L, stats.get("completedTodos"));
        assertEquals(3L, stats.get("pendingTodos"));
    }

    private void mockSecurityContext() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("testuser");
        when(securityContext.getAuthentication()).thenReturn(authentication);

        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
        }

        SecurityContextHolder.setContext(securityContext);
    }
}
