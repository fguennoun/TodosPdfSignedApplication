package be.cm.todoapplication.service.messaging;

import be.cm.todoapplication.dto.messaging.NotificationMessage;
import be.cm.todoapplication.dto.messaging.PdfProcessingMessage;
import be.cm.todoapplication.dto.messaging.TodoSyncMessage;
import be.cm.todoapplication.service.WebSocketNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour KafkaMessageListener
 *
 * Teste le traitement des messages Kafka pour les différents topics
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KafkaMessageListener Tests")
class KafkaMessageListenerTest {

    @Mock
    private WebSocketNotificationService webSocketNotificationService;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private KafkaMessageListener kafkaMessageListener;

    private PdfProcessingMessage pdfMessage;
    private TodoSyncMessage todoSyncMessage;
    private NotificationMessage notificationMessage;

    @BeforeEach
    void setUp() {
        pdfMessage = new PdfProcessingMessage(
                "task-123",
                "user123",
                "todo-456",
                "test.pdf",
                "/path/to/test.pdf",
                PdfProcessingMessage.ProcessingStatus.PROCESSING,
                null,
                LocalDateTime.now()
        );

        todoSyncMessage = new TodoSyncMessage(
                "user123",
                TodoSyncMessage.SyncAction.FETCH_FROM_JSONPLACEHOLDER,
                "batch-789",
                100,
                50,
                TodoSyncMessage.SyncStatus.IN_PROGRESS,
                null,
                LocalDateTime.now()
        );

        notificationMessage = NotificationMessage.create(
                "user123",
                NotificationMessage.NotificationType.TODO_CREATED,
                "Test Title",
                "Test Message"
        );
    }

    @Nested
    @DisplayName("Tests de traitement des messages PDF")
    class PdfProcessingMessageTests {

        @Test
        @DisplayName("Traitement réussi d'un message PDF en cours")
        void handlePdfProcessingMessage_Processing_Success() {
            // Given
            pdfMessage.setStatus(PdfProcessingMessage.ProcessingStatus.PROCESSING);

            // When
            kafkaMessageListener.handlePdfProcessingMessage(pdfMessage, "pdf-processing", 0, acknowledgment);

            // Then
            verify(webSocketNotificationService).sendPdfProcessingUpdate(
                    "user123",
                    "task-123",
                    "PROCESSING",
                    "PDF generation in progress..."
            );
            verify(acknowledgment).acknowledge();
        }

        @Test
        @DisplayName("Traitement d'un message PDF terminé")
        void handlePdfProcessingMessage_Completed_Success() {
            // Given
            pdfMessage.setStatus(PdfProcessingMessage.ProcessingStatus.COMPLETED);

            // When
            kafkaMessageListener.handlePdfProcessingMessage(pdfMessage, "pdf-processing", 0, acknowledgment);

            // Then
            verify(acknowledgment).acknowledge();
            // Pas de notification WebSocket supplémentaire car déjà envoyée directement
        }

        @Test
        @DisplayName("Traitement d'un message PDF en échec")
        void handlePdfProcessingMessage_Failed() {
            // Given
            pdfMessage.setStatus(PdfProcessingMessage.ProcessingStatus.FAILED);
            pdfMessage.setErrorMessage("PDF generation failed");

            // When
            kafkaMessageListener.handlePdfProcessingMessage(pdfMessage, "pdf-processing", 0, acknowledgment);

            // Then
            verify(acknowledgment).acknowledge();
        }

        @Test
        @DisplayName("Gestion d'exception lors du traitement PDF")
        void handlePdfProcessingMessage_Exception() {
            // Given
            doThrow(new RuntimeException("WebSocket error"))
                    .when(webSocketNotificationService).sendPdfProcessingUpdate(anyString(), anyString(), anyString(), anyString());

            pdfMessage.setStatus(PdfProcessingMessage.ProcessingStatus.PROCESSING);

            // When
            kafkaMessageListener.handlePdfProcessingMessage(pdfMessage, "pdf-processing", 0, acknowledgment);

            // Then
            verify(acknowledgment, never()).acknowledge(); // Ne pas acknowledger en cas d'erreur
        }
    }

    @Nested
    @DisplayName("Tests de traitement des messages de synchronisation")
    class TodoSyncMessageTests {

        @Test
        @DisplayName("Traitement réussi d'un message de sync démarré")
        void handleTodoSyncMessage_Started_Success() {
            // Given
            todoSyncMessage.setStatus(TodoSyncMessage.SyncStatus.STARTED);

            // When
            kafkaMessageListener.handleTodoSyncMessage(todoSyncMessage, "todo-sync", 0, acknowledgment);

            // Then
            verify(acknowledgment).acknowledge();
        }

        @Test
        @DisplayName("Traitement d'un message de sync en cours")
        void handleTodoSyncMessage_InProgress_Success() {
            // Given
            todoSyncMessage.setStatus(TodoSyncMessage.SyncStatus.IN_PROGRESS);
            todoSyncMessage.setProcessedTodos(75);

            // When
            kafkaMessageListener.handleTodoSyncMessage(todoSyncMessage, "todo-sync", 0, acknowledgment);

            // Then
            verify(acknowledgment).acknowledge();
        }

        @Test
        @DisplayName("Traitement d'un message de sync terminé")
        void handleTodoSyncMessage_Completed_Success() {
            // Given
            todoSyncMessage.setStatus(TodoSyncMessage.SyncStatus.COMPLETED);
            todoSyncMessage.setProcessedTodos(100);

            // When
            kafkaMessageListener.handleTodoSyncMessage(todoSyncMessage, "todo-sync", 0, acknowledgment);

            // Then
            verify(acknowledgment).acknowledge();
        }

        @Test
        @DisplayName("Traitement d'un message de sync en échec")
        void handleTodoSyncMessage_Failed() {
            // Given
            todoSyncMessage.setStatus(TodoSyncMessage.SyncStatus.FAILED);
            todoSyncMessage.setErrorMessage("Sync failed due to network error");

            // When
            kafkaMessageListener.handleTodoSyncMessage(todoSyncMessage, "todo-sync", 0, acknowledgment);

            // Then
            verify(acknowledgment).acknowledge();
        }

        @Test
        @DisplayName("Gestion d'exception lors du traitement sync")
        void handleTodoSyncMessage_Exception() {
            // Given - Exception simulée
            doThrow(new RuntimeException("Processing error")).when(acknowledgment).acknowledge();

            // When
            kafkaMessageListener.handleTodoSyncMessage(todoSyncMessage, "todo-sync", 0, acknowledgment);

            // Then - L'exception est capturée et loggée
            verify(acknowledgment).acknowledge();
        }
    }

    @Nested
    @DisplayName("Tests de traitement des notifications")
    class NotificationMessageTests {

        @Test
        @DisplayName("Traitement réussi d'une notification utilisateur")
        void handleNotificationMessage_UserNotification_Success() {
            // Given
            notificationMessage = NotificationMessage.create(
                    "user123",
                    NotificationMessage.NotificationType.PDF_PROCESSING_COMPLETED,
                    "PDF Ready",
                    "Your PDF is ready for download"
            );

            // When
            kafkaMessageListener.handleNotificationMessage(notificationMessage, "notifications", 0, acknowledgment);

            // Then
            verify(webSocketNotificationService).sendNotificationToUser("user123", notificationMessage);
            verify(acknowledgment).acknowledge();
        }

        @Test
        @DisplayName("Traitement d'une notification système")
        void handleNotificationMessage_SystemNotification_Success() {
            // Given
            notificationMessage = NotificationMessage.create(
                    "user123",
                    NotificationMessage.NotificationType.SYSTEM_NOTIFICATION,
                    "System Update",
                    "System maintenance scheduled"
            );

            // When
            kafkaMessageListener.handleNotificationMessage(notificationMessage, "notifications", 0, acknowledgment);

            // Then
            verify(webSocketNotificationService).sendNotificationToUser("user123", notificationMessage);
            verify(webSocketNotificationService).sendNotificationToTopic("system", notificationMessage);
            verify(acknowledgment).acknowledge();
        }

        @Test
        @DisplayName("Traitement d'une notification de synchronisation")
        void handleNotificationMessage_SyncCompleted_Success() {
            // Given
            notificationMessage = NotificationMessage.create(
                    "user123",
                    NotificationMessage.NotificationType.SYNC_COMPLETED,
                    "Sync Complete",
                    "Todo synchronization completed successfully"
            );

            // When
            kafkaMessageListener.handleNotificationMessage(notificationMessage, "notifications", 0, acknowledgment);

            // Then
            verify(webSocketNotificationService).sendNotificationToUser("user123", notificationMessage);
            verify(acknowledgment).acknowledge();
        }

        @Test
        @DisplayName("Gestion d'exception lors de l'envoi de notification")
        void handleNotificationMessage_Exception() {
            // Given
            doThrow(new RuntimeException("WebSocket connection failed"))
                    .when(webSocketNotificationService).sendNotificationToUser(anyString(), any(NotificationMessage.class));

            // When
            kafkaMessageListener.handleNotificationMessage(notificationMessage, "notifications", 0, acknowledgment);

            // Then
            verify(acknowledgment, never()).acknowledge(); // Ne pas acknowledger en cas d'erreur
        }

        @Test
        @DisplayName("Traitement de tous les types de notifications")
        void handleNotificationMessage_AllTypes() {
            // Test pour chaque type de notification
            NotificationMessage.NotificationType[] types = {
                    NotificationMessage.NotificationType.TODO_CREATED,
                    NotificationMessage.NotificationType.TODO_UPDATED,
                    NotificationMessage.NotificationType.TODO_DELETED,
                    NotificationMessage.NotificationType.PDF_PROCESSING_COMPLETED,
                    NotificationMessage.NotificationType.PDF_PROCESSING_FAILED,
                    NotificationMessage.NotificationType.SYNC_COMPLETED,
                    NotificationMessage.NotificationType.SYNC_FAILED
            };

            for (NotificationMessage.NotificationType type : types) {
                // Given
                NotificationMessage message = NotificationMessage.create(
                        "user123",
                        type,
                        "Test " + type,
                        "Test message for " + type
                );

                // When
                kafkaMessageListener.handleNotificationMessage(message, "notifications", 0, acknowledgment);

                // Then
                verify(webSocketNotificationService).sendNotificationToUser("user123", message);
            }

            verify(acknowledgment, times(types.length)).acknowledge();
        }
    }

    @Nested
    @DisplayName("Tests d'intégration et de robustesse")
    class IntegrationTests {

        @Test
        @DisplayName("Traitement de messages multiples en séquence")
        void handleMultipleMessages_Sequential() {
            // Given - Messages PDF
            PdfProcessingMessage msg1 = new PdfProcessingMessage(
                    "task-1", "user1", "todo1", "file1.pdf", "/path1",
                    PdfProcessingMessage.ProcessingStatus.PROCESSING, null, LocalDateTime.now()
            );
            PdfProcessingMessage msg2 = new PdfProcessingMessage(
                    "task-2", "user2", "todo2", "file2.pdf", "/path2",
                    PdfProcessingMessage.ProcessingStatus.COMPLETED, null, LocalDateTime.now()
            );

            // When
            kafkaMessageListener.handlePdfProcessingMessage(msg1, "pdf-processing", 0, acknowledgment);
            kafkaMessageListener.handlePdfProcessingMessage(msg2, "pdf-processing", 1, acknowledgment);

            // Then
            verify(webSocketNotificationService).sendPdfProcessingUpdate("user1", "task-1", "PROCESSING", "PDF generation in progress...");
            verify(acknowledgment, times(2)).acknowledge();
        }

        @Test
        @DisplayName("Résilience face aux messages malformés")
        void handleMalformedMessage_Resilience() {
            // Given - Message avec données nulles
            PdfProcessingMessage malformedMessage = new PdfProcessingMessage(
                    null, null, null, null, null, null, null, null
            );

            // When & Then - Ne doit pas lever d'exception
            kafkaMessageListener.handlePdfProcessingMessage(malformedMessage, "pdf-processing", 0, acknowledgment);

            verify(acknowledgment).acknowledge();
        }

        @Test
        @DisplayName("Performance avec messages volumineux")
        void handleLargeMessage_Performance() {
            // Given - Message avec beaucoup de données
            String largeErrorMessage = "Error: " + "x".repeat(10000);
            pdfMessage.setStatus(PdfProcessingMessage.ProcessingStatus.FAILED);
            pdfMessage.setErrorMessage(largeErrorMessage);

            // When
            long startTime = System.currentTimeMillis();
            kafkaMessageListener.handlePdfProcessingMessage(pdfMessage, "pdf-processing", 0, acknowledgment);
            long endTime = System.currentTimeMillis();

            // Then
            verify(acknowledgment).acknowledge();
            // Le traitement devrait être rapide même avec des messages volumineux
            org.assertj.core.api.Assertions.assertThat(endTime - startTime).isLessThan(1000); // Moins d'1 seconde
        }
    }
}
