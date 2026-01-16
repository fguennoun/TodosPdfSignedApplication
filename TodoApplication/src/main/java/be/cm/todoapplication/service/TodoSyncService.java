package be.cm.todoapplication.service;

import be.cm.todoapplication.dto.TodoDTO;
import be.cm.todoapplication.dto.messaging.NotificationMessage;
import be.cm.todoapplication.dto.messaging.TodoSyncMessage;
import be.cm.todoapplication.model.Todo;
import be.cm.todoapplication.model.User;
import be.cm.todoapplication.repository.TodoRepository;
import be.cm.todoapplication.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class TodoSyncService {

    private final TodoRepository todoRepository;
    private final UserRepository userRepository;
    private final KafkaMessageService kafkaMessageService;
    private final WebSocketNotificationService webSocketNotificationService;

    /**
     * Synchronise un todo depuis JSONPlaceholder avec retry automatique
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void syncTodo(TodoDTO dto, Map<Long, String> userMap) {
        int maxRetries = 3;
        int attempts = 0;

        while (attempts < maxRetries) {
            try {
                // Trouver ou créer l'utilisateur
                String username = userMap.get(dto.getUserId());
                if (username == null) {
                    log.warn("Utilisateur non trouvé pour userId: {}", dto.getUserId());
                    return;
                }

                User user = findOrCreateUser(dto.getUserId(), username);

                // Synchroniser le todo
                Optional<Todo> existingTodoOptional = todoRepository.findById(dto.getId());
                if (existingTodoOptional.isPresent()) {
                    updateExistingTodo(existingTodoOptional.get(), dto, user);
                } else {
                    createNewTodo(dto, user);
                }

                log.debug("Todo {} synchronisé avec succès", dto.getId());
                return; // Succès

            } catch (OptimisticLockingFailureException e) {
                attempts++;
                log.warn("Conflit de verrouillage optimiste pour todo {} - tentative {}/{}",
                        dto.getId(), attempts, maxRetries);

                if (attempts >= maxRetries) {
                    log.error("Échec de synchronisation du todo {} après {} tentatives",
                             dto.getId(), maxRetries);
                    return;
                }

                try {
                    Thread.sleep(100 * attempts); // Backoff progressif
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.error("Thread interrompu pendant la synchronisation du todo {}", dto.getId());
                    return;
                }
            } catch (Exception e) {
                log.error("Erreur lors de la synchronisation du todo {}: {}", dto.getId(), e.getMessage(), e);
                return;
            }
        }
    }

    /**
     * Trouve un utilisateur existant ou en crée un nouveau pour la synchronisation
     */
    private User findOrCreateUser(Long userId, String username) {
        // Chercher d'abord par username (plus fiable)
        Optional<User> existingUser = userRepository.findByUsername(username);
        if (existingUser.isPresent()) {
            return existingUser.get();
        }

        // Créer un utilisateur temporaire pour la synchronisation JSONPlaceholder
        User newUser = User.builder()
                .username(username)
                .email(username.toLowerCase() + "@jsonplaceholder.fake")
                .password("$2a$10$disabled") // Mot de passe désactivé
                .role(User.Role.USER)
                .enabled(false) // Compte désactivé car c'est juste pour la sync
                .build();

        return userRepository.save(newUser);
    }

    /**
     * Met à jour un todo existant
     */
    private void updateExistingTodo(Todo existingTodo, TodoDTO dto, User user) {
        existingTodo.setTitle(dto.getTitle());
        existingTodo.setCompleted(dto.getCompleted());
        existingTodo.setUser(user);
        existingTodo.setUpdatedBy("SYSTEM_SYNC");

        todoRepository.save(existingTodo);
    }

    /**
     * Crée un nouveau todo
     */
    private void createNewTodo(TodoDTO dto, User user) {
        Todo newTodo = Todo.builder()
                .id(dto.getId()) // Garder l'ID de JSONPlaceholder
                .title(dto.getTitle())
                .completed(dto.getCompleted())
                .user(user)
                .createdBy("SYSTEM_SYNC")
                .build();

        todoRepository.save(newTodo);
    }

    /**
     * Synchronisation asynchrone en lot avec notifications temps réel
     */
    @Async("todoSyncExecutor")
    public CompletableFuture<String> syncTodosBatchAsync(String userId, List<TodoDTO> todos, Map<Long, String> userMap) {
        String batchId = UUID.randomUUID().toString();

        try {
            log.info("Starting async batch sync for user: {} with batch: {}", userId, batchId);

            // Message de début de synchronisation
            TodoSyncMessage startMessage = TodoSyncMessage.createStart(userId, batchId);
            kafkaMessageService.sendTodoSyncMessage(startMessage);

            // Notification WebSocket de début
            webSocketNotificationService.sendTodoSyncUpdate(userId, batchId, 0, todos.size());

            int processed = 0;
            int batchSize = 10; // Traiter par lots de 10

            for (int i = 0; i < todos.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, todos.size());
                List<TodoDTO> batch = todos.subList(i, endIndex);

                // Traiter le lot
                for (TodoDTO todo : batch) {
                    try {
                        syncTodo(todo, userMap);
                        processed++;

                        // Notification de progression toutes les 5 todos
                        if (processed % 5 == 0) {
                            webSocketNotificationService.sendTodoSyncUpdate(userId, batchId, processed, todos.size());
                        }

                    } catch (Exception e) {
                        log.error("Error syncing todo {}: {}", todo.getId(), e.getMessage());
                    }
                }

                // Pause entre les lots pour éviter la surcharge
                Thread.sleep(100);
            }

            // Message de fin de synchronisation
            TodoSyncMessage completedMessage = new TodoSyncMessage(
                userId, TodoSyncMessage.SyncAction.COMPLETE_SYNC, batchId,
                todos.size(), processed, TodoSyncMessage.SyncStatus.COMPLETED,
                null, java.time.LocalDateTime.now()
            );
            kafkaMessageService.sendTodoSyncMessage(completedMessage);

            // Notification finale
            webSocketNotificationService.sendTodoSyncUpdate(userId, batchId, processed, todos.size());

            // Notification générale
            NotificationMessage notification = NotificationMessage.create(
                userId,
                NotificationMessage.NotificationType.SYNC_COMPLETED,
                "Synchronization Complete",
                String.format("Successfully synchronized %d/%d todos", processed, todos.size()),
                batchId
            );
            kafkaMessageService.sendNotificationMessage(notification);

            log.info("Completed async batch sync for user: {} with batch: {}, processed: {}/{}",
                    userId, batchId, processed, todos.size());

            return CompletableFuture.completedFuture(batchId);

        } catch (Exception e) {
            log.error("Error in async batch sync for user: {} with batch: {}", userId, batchId, e);

            // Message d'erreur
            TodoSyncMessage failedMessage = new TodoSyncMessage(
                userId, TodoSyncMessage.SyncAction.COMPLETE_SYNC, batchId,
                todos.size(), 0, TodoSyncMessage.SyncStatus.FAILED,
                e.getMessage(), java.time.LocalDateTime.now()
            );
            kafkaMessageService.sendTodoSyncMessage(failedMessage);

            // Notification d'erreur
            NotificationMessage errorNotification = NotificationMessage.create(
                userId,
                NotificationMessage.NotificationType.SYNC_FAILED,
                "Synchronization Failed",
                "An error occurred during todo synchronization: " + e.getMessage(),
                batchId
            );
            kafkaMessageService.sendNotificationMessage(errorNotification);

            return CompletableFuture.failedFuture(e);
        }
    }
}

