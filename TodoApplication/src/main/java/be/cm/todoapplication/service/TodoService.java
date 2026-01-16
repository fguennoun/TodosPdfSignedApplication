package be.cm.todoapplication.service;

import be.cm.todoapplication.dto.TodoDTO;
import be.cm.todoapplication.dto.UserDTO;
import be.cm.todoapplication.dto.messaging.NotificationMessage;
import be.cm.todoapplication.model.Todo;
import be.cm.todoapplication.model.User;
import be.cm.todoapplication.repository.TodoRepository;
import be.cm.todoapplication.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TodoService {

    private final TodoRepository todoRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final TodoSyncService todoSyncService;
    private final KafkaMessageService kafkaMessageService;
    private final WebSocketNotificationService webSocketNotificationService;

    private static final String TODOS_API = "https://jsonplaceholder.typicode.com/todos";
    private static final String USERS_API = "https://jsonplaceholder.typicode.com/users";

    /**
     * Synchronisation avec JSONPlaceholder - pour admin uniquement
     */
    @Transactional
    public void syncFromJsonPlaceholder() {
        try {
            log.info("Démarrage de la synchronisation JSONPlaceholder");

            TodoDTO[] todoDTOs = restTemplate.getForObject(TODOS_API, TodoDTO[].class);
            UserDTO[] userDTOs = restTemplate.getForObject(USERS_API, UserDTO[].class);

            if (todoDTOs == null || userDTOs == null) {
                log.error("Échec de récupération des données JSONPlaceholder");
                throw new RuntimeException("Impossible de récupérer les données JSONPlaceholder");
            }

            Map<Long, String> userMap = Arrays.stream(userDTOs)
                    .collect(Collectors.toMap(UserDTO::getId, UserDTO::getName));

            int syncCount = 0;
            for (TodoDTO dto : todoDTOs) {
                try {
                    todoSyncService.syncTodo(dto, userMap);
                    syncCount++;
                } catch (Exception e) {
                    log.warn("Erreur sync todo ID {}: {}", dto.getId(), e.getMessage());
                }
            }

            log.info("Synchronisation terminée: {} todos traités", syncCount);
        } catch (Exception e) {
            log.error("Erreur lors de la synchronisation: {}", e.getMessage());
            throw new RuntimeException("Erreur de synchronisation: " + e.getMessage());
        }
    }

    /**
     * Récupère tous les todos de l'utilisateur connecté avec pagination
     */
    @Transactional(readOnly = true)
    public Page<TodoDTO> getUserTodos(Pageable pageable, Boolean completed, String search) {
        User currentUser = getCurrentUser();
        Page<Todo> todos;

        if (search != null && !search.trim().isEmpty()) {
            todos = todoRepository.findByUserAndTitleContaining(currentUser, search.trim(), pageable);
        } else {
            todos = todoRepository.findByUserWithOptionalCompleted(currentUser, completed, pageable);
        }

        List<TodoDTO> todoDTOs = todos.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return new PageImpl<>(todoDTOs, pageable, todos.getTotalElements());
    }

    /**
     * Récupère un todo par ID (vérifie l'ownership)
     */
    @Transactional(readOnly = true)
    public Optional<TodoDTO> getTodoById(Long id) {
        User currentUser = getCurrentUser();
        return todoRepository.findByIdAndUser(id, currentUser)
                .map(this::convertToDTO);
    }

    /**
     * Crée un nouveau todo
     */
    @Transactional
    public TodoDTO createTodo(TodoDTO todoDTO) {
        User currentUser = getCurrentUser();

        Todo todo = Todo.builder()
                .title(todoDTO.getTitle())
                .description(todoDTO.getDescription())
                .completed(todoDTO.getCompleted() != null ? todoDTO.getCompleted() : false)
                .user(currentUser)
                .createdBy(currentUser.getUsername())
                .build();

        Todo savedTodo = todoRepository.save(todo);
        log.info("Nouveau todo créé: {} par {}", savedTodo.getId(), currentUser.getUsername());

        return convertToDTO(savedTodo);
    }

    /**
     * Met à jour un todo existant
     */
    @Transactional
    public TodoDTO updateTodo(Long id, TodoDTO todoDTO) {
        User currentUser = getCurrentUser();
        Todo existingTodo = todoRepository.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new RuntimeException("Todo non trouvé ou accès non autorisé"));

        // Mise à jour des champs
        existingTodo.setTitle(todoDTO.getTitle());
        existingTodo.setDescription(todoDTO.getDescription());
        existingTodo.setCompleted(todoDTO.getCompleted());
        existingTodo.setUpdatedBy(currentUser.getUsername());

        Todo updatedTodo = todoRepository.save(existingTodo);
        log.info("Todo {} mis à jour par {}", id, currentUser.getUsername());

        return convertToDTO(updatedTodo);
    }

    /**
     * Supprime un todo
     */
    @Transactional
    public void deleteTodo(Long id) {
        User currentUser = getCurrentUser();
        Todo todo = todoRepository.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new RuntimeException("Todo non trouvé ou accès non autorisé"));

        todoRepository.delete(todo);
        log.info("Todo {} supprimé par {}", id, currentUser.getUsername());
    }

    /**
     * Statistiques utilisateur
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getUserStats() {
        User currentUser = getCurrentUser();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTodos", todoRepository.countByUserAndCompleted(currentUser, null));
        stats.put("completedTodos", todoRepository.countByUserAndCompleted(currentUser, true));
        stats.put("pendingTodos", todoRepository.countByUserAndCompleted(currentUser, false));

        return stats;
    }

    /**
     * Récupère l'utilisateur connecté
     */
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Utilisateur non authentifié");
        }

        String username = auth.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé: " + username));
    }

    /**
     * Convertit une entité Todo en DTO
     */
    private TodoDTO convertToDTO(Todo todo) {
        return TodoDTO.builder()
                .id(todo.getId())
                .title(todo.getTitle())
                .description(todo.getDescription())
                .completed(todo.getCompleted())
                .userId(todo.getUserId())
                .username(todo.getUsername())
                .createdAt(todo.getCreatedAt())
                .updatedAt(todo.getUpdatedAt())
                .createdBy(todo.getCreatedBy())
                .updatedBy(todo.getUpdatedBy())
                .build();
    }

    /**
     * Synchronisation asynchrone avec JSONPlaceholder
     */
    @Async("todoSyncExecutor")
    public CompletableFuture<String> syncFromJsonPlaceholderAsync(String adminUsername) {
        try {
            log.info("Démarrage de la synchronisation asynchrone JSONPlaceholder par: {}", adminUsername);

            TodoDTO[] todoDTOs = restTemplate.getForObject(TODOS_API, TodoDTO[].class);
            UserDTO[] userDTOs = restTemplate.getForObject(USERS_API, UserDTO[].class);

            if (todoDTOs == null || userDTOs == null) {
                log.error("Échec de récupération des données JSONPlaceholder");
                throw new RuntimeException("Impossible de récupérer les données JSONPlaceholder");
            }

            // Créer la map des utilisateurs
            Map<Long, String> userMap = Arrays.stream(userDTOs)
                    .collect(Collectors.toMap(UserDTO::getId, UserDTO::getUsername));

            // Utiliser le service de synchronisation asynchrone
            List<TodoDTO> todoList = Arrays.asList(todoDTOs);
            CompletableFuture<String> syncResult = todoSyncService.syncTodosBatchAsync(adminUsername, todoList, userMap);

            log.info("Synchronisation asynchrone démarrée avec succès");
            return syncResult;

        } catch (Exception e) {
            log.error("Erreur lors de la synchronisation asynchrone: {}", e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Récupère tous les todos d'un utilisateur
     */
    public List<TodoDTO> getAllTodosByUser(String username) {
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé: " + username));

            List<Todo> todos = todoRepository.findByUser(user);
            return todos.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des todos pour l'utilisateur {}: {}", username, e.getMessage());
            throw e;
        }
    }

    /**
     * Envoie une notification de test via WebSocket
     */
    public void sendTestNotification(String username) {
        try {
            NotificationMessage testNotification = NotificationMessage.create(
                username,
                NotificationMessage.NotificationType.SYSTEM_NOTIFICATION,
                "Test Notification",
                "This is a test notification to verify WebSocket connectivity"
            );

            // Envoyer via Kafka
            kafkaMessageService.sendNotificationMessage(testNotification);

            // Envoyer directement via WebSocket
            webSocketNotificationService.sendNotificationToUser(username, testNotification);

            log.info("Notification de test envoyée à l'utilisateur: {}", username);

        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de la notification de test pour {}: {}", username, e.getMessage());
            throw e;
        }
    }
}
