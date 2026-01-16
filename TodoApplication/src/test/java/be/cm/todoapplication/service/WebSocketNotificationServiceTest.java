package be.cm.todoapplication.service;

import be.cm.todoapplication.dto.messaging.NotificationMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour WebSocketNotificationService
 *
 * Teste l'envoi de notifications WebSocket vers les utilisateurs et topics
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WebSocketNotificationService Tests")
class WebSocketNotificationServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private WebSocketNotificationService webSocketNotificationService;

    private NotificationMessage testNotification;

    @BeforeEach
    void setUp() {
        testNotification = NotificationMessage.create(
                "user123",
                NotificationMessage.NotificationType.TODO_CREATED,
                "Test Notification",
                "This is a test notification"
        );
    }

    @Nested
    @DisplayName("Tests d'envoi de notifications utilisateur")
    class UserNotificationTests {

        @Test
        @DisplayName("Envoi réussi de notification à un utilisateur")
        void sendNotificationToUser_Success() {
            // When
            webSocketNotificationService.sendNotificationToUser("user123", testNotification);

            // Then
            verify(messagingTemplate).convertAndSendToUser(
                    "user123",
                    "/queue/notifications",
                    testNotification
            );
        }

        @Test
        @DisplayName("Envoi de notification avec utilisateur null")
        void sendNotificationToUser_NullUser() {
            // When
            webSocketNotificationService.sendNotificationToUser(null, testNotification);

            // Then
            verify(messagingTemplate).convertAndSendToUser(
                    isNull(),
                    eq("/queue/notifications"),
                    eq(testNotification)
            );
        }

        @Test
        @DisplayName("Envoi de notification avec message null")
        void sendNotificationToUser_NullNotification() {
            // When
            webSocketNotificationService.sendNotificationToUser("user123", null);

            // Then
            verify(messagingTemplate).convertAndSendToUser(
                    eq("user123"),
                    eq("/queue/notifications"),
                    isNull()
            );
        }

        @Test
        @DisplayName("Gestion d'exception lors de l'envoi à un utilisateur")
        void sendNotificationToUser_Exception() {
            // Given
            doThrow(new RuntimeException("WebSocket connection failed"))
                    .when(messagingTemplate).convertAndSendToUser(anyString(), anyString(), any());

            // When - Ne doit pas lever d'exception
            webSocketNotificationService.sendNotificationToUser("user123", testNotification);

            // Then
            verify(messagingTemplate).convertAndSendToUser("user123", "/queue/notifications", testNotification);
        }

        @Test
        @DisplayName("Envoi de notifications à plusieurs utilisateurs")
        void sendNotificationToMultipleUsers() {
            // Given
            String[] users = {"user1", "user2", "user3"};

            // When
            for (String user : users) {
                webSocketNotificationService.sendNotificationToUser(user, testNotification);
            }

            // Then
            for (String user : users) {
                verify(messagingTemplate).convertAndSendToUser(user, "/queue/notifications", testNotification);
            }
        }
    }

    @Nested
    @DisplayName("Tests d'envoi de notifications topic")
    class TopicNotificationTests {

        @Test
        @DisplayName("Envoi réussi de notification à un topic")
        void sendNotificationToTopic_Success() {
            // When
            webSocketNotificationService.sendNotificationToTopic("system", testNotification);

            // Then
            verify(messagingTemplate).convertAndSend(
                    "/topic/system",
                    testNotification
            );
        }

        @Test
        @DisplayName("Envoi de notification à topic avec nom null")
        void sendNotificationToTopic_NullTopic() {
            // When
            webSocketNotificationService.sendNotificationToTopic(null, testNotification);

            // Then
            verify(messagingTemplate).convertAndSend(
                    "/topic/null",
                    testNotification
            );
        }

        @Test
        @DisplayName("Gestion d'exception lors de l'envoi à un topic")
        void sendNotificationToTopic_Exception() {
            // Given
            doThrow(new RuntimeException("Topic broadcast failed"))
                    .when(messagingTemplate).convertAndSend(anyString(), any(NotificationMessage.class));

            // When - Ne doit pas lever d'exception
            webSocketNotificationService.sendNotificationToTopic("system", testNotification);

            // Then
            verify(messagingTemplate).convertAndSend("/topic/system", testNotification);
        }

        @Test
        @DisplayName("Envoi à plusieurs topics")
        void sendNotificationToMultipleTopics() {
            // Given
            String[] topics = {"system", "alerts", "updates"};

            // When
            for (String topic : topics) {
                webSocketNotificationService.sendNotificationToTopic(topic, testNotification);
            }

            // Then
            for (String topic : topics) {
                verify(messagingTemplate).convertAndSend("/topic/" + topic, testNotification);
            }
        }
    }

    @Nested
    @DisplayName("Tests des méthodes spécialisées")
    class SpecializedMethodTests {

        @Test
        @DisplayName("Envoi de mise à jour de traitement PDF réussi")
        void sendPdfProcessingUpdate_Success() {
            // When
            webSocketNotificationService.sendPdfProcessingUpdate("user123", "task-456", "COMPLETED", "PDF generation completed");

            // Then
            verify(messagingTemplate).convertAndSendToUser(
                    eq("user123"),
                    eq("/queue/notifications"),
                    argThat(notification -> {
                        NotificationMessage msg = (NotificationMessage) notification;
                        return msg.getUserId().equals("user123") &&
                                msg.getType() == NotificationMessage.NotificationType.PDF_PROCESSING_COMPLETED &&
                                msg.getTitle().equals("PDF Processing Update") &&
                                msg.getMessage().equals("PDF generation completed") &&
                                msg.getData() instanceof WebSocketNotificationService.PdfProcessingUpdateData;
                    })
            );
        }

        @Test
        @DisplayName("Envoi de mise à jour de traitement PDF avec données invalides")
        void sendPdfProcessingUpdate_InvalidData() {
            // When
            webSocketNotificationService.sendPdfProcessingUpdate(null, "", "FAILED", null);

            // Then
            verify(messagingTemplate).convertAndSendToUser(
                    isNull(),
                    eq("/queue/notifications"),
                    any(NotificationMessage.class)
            );
        }

        @Test
        @DisplayName("Envoi de mise à jour de synchronisation Todo")
        void sendTodoSyncUpdate_Success() {
            // When
            webSocketNotificationService.sendTodoSyncUpdate("user123", "batch-789", 50, 100);

            // Then
            verify(messagingTemplate).convertAndSendToUser(
                    eq("user123"),
                    eq("/queue/notifications"),
                    argThat(notification -> {
                        NotificationMessage msg = (NotificationMessage) notification;
                        return msg.getUserId().equals("user123") &&
                                msg.getType() == NotificationMessage.NotificationType.SYNC_COMPLETED &&
                                msg.getTitle().equals("Todo Sync Progress") &&
                                msg.getMessage().equals("Synchronized 50/100 todos") &&
                                msg.getData() instanceof WebSocketNotificationService.TodoSyncUpdateData;
                    })
            );
        }

        @Test
        @DisplayName("Envoi de mise à jour de synchronisation Todo - progression complète")
        void sendTodoSyncUpdate_Complete() {
            // When
            webSocketNotificationService.sendTodoSyncUpdate("user123", "batch-789", 100, 100);

            // Then
            verify(messagingTemplate).convertAndSendToUser(
                    eq("user123"),
                    eq("/queue/notifications"),
                    argThat(notification -> {
                        NotificationMessage msg = (NotificationMessage) notification;
                        return msg.getMessage().equals("Synchronized 100/100 todos");
                    })
            );
        }

        @Test
        @DisplayName("Vérification des données structurées dans les notifications")
        void verifyStructuredData() {
            // When - PDF Update
            webSocketNotificationService.sendPdfProcessingUpdate("user1", "task1", "PROCESSING", "Processing...");

            // When - Sync Update
            webSocketNotificationService.sendTodoSyncUpdate("user2", "batch1", 25, 50);

            // Then - Vérifier que les données sont correctement structurées
            verify(messagingTemplate).convertAndSendToUser(
                    eq("user1"),
                    eq("/queue/notifications"),
                    argThat(notification -> {
                        NotificationMessage msg = (NotificationMessage) notification;
                        WebSocketNotificationService.PdfProcessingUpdateData data =
                                (WebSocketNotificationService.PdfProcessingUpdateData) msg.getData();
                        return data.taskId().equals("task1") && data.status().equals("PROCESSING");
                    })
            );

            verify(messagingTemplate).convertAndSendToUser(
                    eq("user2"),
                    eq("/queue/notifications"),
                    argThat(notification -> {
                        NotificationMessage msg = (NotificationMessage) notification;
                        WebSocketNotificationService.TodoSyncUpdateData data =
                                (WebSocketNotificationService.TodoSyncUpdateData) msg.getData();
                        return data.batchId().equals("batch1") &&
                                data.processed() == 25 &&
                                data.total() == 50;
                    })
            );
        }
    }

    @Nested
    @DisplayName("Tests d'intégration et performance")
    class IntegrationTests {

        @Test
        @DisplayName("Envoi simultané à utilisateur et topic")
        void sendToUserAndTopic_Simultaneously() {
            // Given
            NotificationMessage systemNotification = NotificationMessage.create(
                    "user123",
                    NotificationMessage.NotificationType.SYSTEM_NOTIFICATION,
                    "System Alert",
                    "Important system message"
            );

            // When
            webSocketNotificationService.sendNotificationToUser("user123", systemNotification);
            webSocketNotificationService.sendNotificationToTopic("system", systemNotification);

            // Then
            verify(messagingTemplate).convertAndSendToUser("user123", "/queue/notifications", systemNotification);
            verify(messagingTemplate).convertAndSend("/topic/system", systemNotification);
        }

        @Test
        @DisplayName("Performance avec notifications multiples")
        void performanceTest_MultipleNotifications() {
            // Given
            int notificationCount = 100;

            // When
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < notificationCount; i++) {
                NotificationMessage notification = NotificationMessage.create(
                        "user" + i,
                        NotificationMessage.NotificationType.TODO_CREATED,
                        "Notification " + i,
                        "Message " + i
                );
                webSocketNotificationService.sendNotificationToUser("user" + i, notification);
            }
            long endTime = System.currentTimeMillis();

            // Then
            verify(messagingTemplate, times(notificationCount)).convertAndSendToUser(
                    anyString(), eq("/queue/notifications"), any(NotificationMessage.class)
            );

            // Le traitement devrait être rapide
            long duration = endTime - startTime;
            System.out.println("Processed " + notificationCount + " notifications in " + duration + "ms");
        }

        @Test
        @DisplayName("Résilience avec template messaging défaillant")
        void resilience_MessagingTemplateFailure() {
            // Given
            doThrow(new RuntimeException("Connection lost"))
                    .doNothing() // Recovery après première exception
                    .when(messagingTemplate).convertAndSendToUser(anyString(), anyString(), any());

            // When - Première tentative échoue, deuxième réussit
            webSocketNotificationService.sendNotificationToUser("user1", testNotification);
            webSocketNotificationService.sendNotificationToUser("user2", testNotification);

            // Then
            verify(messagingTemplate, times(2)).convertAndSendToUser(
                    anyString(), eq("/queue/notifications"), any()
            );
        }

        @Test
        @DisplayName("Validation des types de notifications supportés")
        void validateAllNotificationTypes() {
            // Given - Tous les types de notifications
            NotificationMessage.NotificationType[] types = NotificationMessage.NotificationType.values();

            // When
            for (NotificationMessage.NotificationType type : types) {
                NotificationMessage notification = NotificationMessage.create(
                        "user123",
                        type,
                        "Test " + type.name(),
                        "Testing notification type: " + type.name()
                );
                webSocketNotificationService.sendNotificationToUser("user123", notification);
            }

            // Then
            verify(messagingTemplate, times(types.length)).convertAndSendToUser(
                    eq("user123"),
                    eq("/queue/notifications"),
                    any(NotificationMessage.class)
            );
        }
    }
}
