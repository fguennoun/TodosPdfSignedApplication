package be.cm.todoapplication.service;

import be.cm.todoapplication.config.KafkaConfig;
import be.cm.todoapplication.dto.messaging.NotificationMessage;
import be.cm.todoapplication.dto.messaging.PdfProcessingMessage;
import be.cm.todoapplication.dto.messaging.TodoSyncMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour KafkaMessageService
 *
 * Teste l'envoi de messages vers les différents topics Kafka
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KafkaMessageService Tests")
class KafkaMessageServiceTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private CompletableFuture<SendResult<String, Object>> mockFuture;

    @Mock
    private SendResult<String, Object> mockSendResult;

    @InjectMocks
    private KafkaMessageService kafkaMessageService;

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
                NotificationMessage.NotificationType.PDF_PROCESSING_COMPLETED,
                "PDF Ready",
                "Your PDF has been generated successfully"
        );
    }

    @Nested
    @DisplayName("Tests d'envoi de messages PDF")
    class PdfProcessingMessageTests {

        @Test
        @DisplayName("Envoi réussi d'un message de traitement PDF")
        void sendPdfProcessingMessage_Success() {
            // Given
            when(kafkaTemplate.send(eq(KafkaConfig.PDF_PROCESSING_TOPIC), eq("task-123"), eq(pdfMessage)))
                    .thenReturn(mockFuture);
            when(mockFuture.whenComplete(any())).thenReturn(mockFuture);

            // When
            kafkaMessageService.sendPdfProcessingMessage(pdfMessage);

            // Then
            verify(kafkaTemplate).send(KafkaConfig.PDF_PROCESSING_TOPIC, "task-123", pdfMessage);
            verify(mockFuture).whenComplete(any());
        }

        @Test
        @DisplayName("Envoi de message PDF avec taskId null")
        void sendPdfProcessingMessage_NullTaskId() {
            // Given
            pdfMessage.setTaskId(null);
            when(kafkaTemplate.send(eq(KafkaConfig.PDF_PROCESSING_TOPIC), isNull(), eq(pdfMessage)))
                    .thenReturn(mockFuture);
            when(mockFuture.whenComplete(any())).thenReturn(mockFuture);

            // When
            kafkaMessageService.sendPdfProcessingMessage(pdfMessage);

            // Then
            verify(kafkaTemplate).send(KafkaConfig.PDF_PROCESSING_TOPIC, null, pdfMessage);
        }

        @Test
        @DisplayName("Gestion du callback de succès pour PDF")
        void sendPdfProcessingMessage_SuccessCallback() {
            // Given
            when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(mockFuture);
            when(mockFuture.whenComplete(any())).thenAnswer(invocation -> {
                // Simuler l'appel du callback avec succès
                java.util.function.BiConsumer<SendResult<String, Object>, Throwable> callback =
                        invocation.getArgument(0);
                callback.accept(mockSendResult, null);
                return mockFuture;
            });

            // When
            kafkaMessageService.sendPdfProcessingMessage(pdfMessage);

            // Then
            verify(kafkaTemplate).send(KafkaConfig.PDF_PROCESSING_TOPIC, "task-123", pdfMessage);
        }

        @Test
        @DisplayName("Gestion du callback d'erreur pour PDF")
        void sendPdfProcessingMessage_ErrorCallback() {
            // Given
            when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(mockFuture);
            when(mockFuture.whenComplete(any())).thenAnswer(invocation -> {
                // Simuler l'appel du callback avec erreur
                java.util.function.BiConsumer<SendResult<String, Object>, Throwable> callback =
                        invocation.getArgument(0);
                callback.accept(null, new RuntimeException("Kafka send failed"));
                return mockFuture;
            });

            // When
            kafkaMessageService.sendPdfProcessingMessage(pdfMessage);

            // Then
            verify(kafkaTemplate).send(KafkaConfig.PDF_PROCESSING_TOPIC, "task-123", pdfMessage);
        }

        @Test
        @DisplayName("Envoi de messages PDF avec différents statuts")
        void sendPdfProcessingMessage_DifferentStatuses() {
            // Given
            PdfProcessingMessage.ProcessingStatus[] statuses = PdfProcessingMessage.ProcessingStatus.values();
            when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(mockFuture);
            when(mockFuture.whenComplete(any())).thenReturn(mockFuture);

            // When
            for (PdfProcessingMessage.ProcessingStatus status : statuses) {
                pdfMessage.setStatus(status);
                kafkaMessageService.sendPdfProcessingMessage(pdfMessage);
            }

            // Then
            verify(kafkaTemplate, times(statuses.length)).send(
                    eq(KafkaConfig.PDF_PROCESSING_TOPIC),
                    eq("task-123"),
                    any(PdfProcessingMessage.class)
            );
        }
    }

    @Nested
    @DisplayName("Tests d'envoi de messages de synchronisation")
    class TodoSyncMessageTests {

        @Test
        @DisplayName("Envoi réussi d'un message de synchronisation")
        void sendTodoSyncMessage_Success() {
            // Given
            when(kafkaTemplate.send(eq(KafkaConfig.TODO_SYNC_TOPIC), eq("user123"), eq(todoSyncMessage)))
                    .thenReturn(mockFuture);
            when(mockFuture.whenComplete(any())).thenReturn(mockFuture);

            // When
            kafkaMessageService.sendTodoSyncMessage(todoSyncMessage);

            // Then
            verify(kafkaTemplate).send(KafkaConfig.TODO_SYNC_TOPIC, "user123", todoSyncMessage);
            verify(mockFuture).whenComplete(any());
        }

        @Test
        @DisplayName("Envoi de message sync avec userId null")
        void sendTodoSyncMessage_NullUserId() {
            // Given
            todoSyncMessage.setUserId(null);
            when(kafkaTemplate.send(eq(KafkaConfig.TODO_SYNC_TOPIC), isNull(), eq(todoSyncMessage)))
                    .thenReturn(mockFuture);
            when(mockFuture.whenComplete(any())).thenReturn(mockFuture);

            // When
            kafkaMessageService.sendTodoSyncMessage(todoSyncMessage);

            // Then
            verify(kafkaTemplate).send(KafkaConfig.TODO_SYNC_TOPIC, null, todoSyncMessage);
        }

        @Test
        @DisplayName("Envoi de messages sync avec différents statuts")
        void sendTodoSyncMessage_DifferentStatuses() {
            // Given
            TodoSyncMessage.SyncStatus[] statuses = TodoSyncMessage.SyncStatus.values();
            when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(mockFuture);
            when(mockFuture.whenComplete(any())).thenReturn(mockFuture);

            // When
            for (TodoSyncMessage.SyncStatus status : statuses) {
                todoSyncMessage.setStatus(status);
                kafkaMessageService.sendTodoSyncMessage(todoSyncMessage);
            }

            // Then
            verify(kafkaTemplate, times(statuses.length)).send(
                    eq(KafkaConfig.TODO_SYNC_TOPIC),
                    eq("user123"),
                    any(TodoSyncMessage.class)
            );
        }

        @Test
        @DisplayName("Envoi de messages sync avec différentes actions")
        void sendTodoSyncMessage_DifferentActions() {
            // Given
            TodoSyncMessage.SyncAction[] actions = TodoSyncMessage.SyncAction.values();
            when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(mockFuture);
            when(mockFuture.whenComplete(any())).thenReturn(mockFuture);

            // When
            for (TodoSyncMessage.SyncAction action : actions) {
                todoSyncMessage.setAction(action);
                kafkaMessageService.sendTodoSyncMessage(todoSyncMessage);
            }

            // Then
            verify(kafkaTemplate, times(actions.length)).send(
                    eq(KafkaConfig.TODO_SYNC_TOPIC),
                    eq("user123"),
                    any(TodoSyncMessage.class)
            );
        }

        @Test
        @DisplayName("Gestion du callback d'erreur pour sync")
        void sendTodoSyncMessage_ErrorCallback() {
            // Given
            when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(mockFuture);
            when(mockFuture.whenComplete(any())).thenAnswer(invocation -> {
                java.util.function.BiConsumer<SendResult<String, Object>, Throwable> callback =
                        invocation.getArgument(0);
                callback.accept(null, new RuntimeException("Network timeout"));
                return mockFuture;
            });

            // When
            kafkaMessageService.sendTodoSyncMessage(todoSyncMessage);

            // Then
            verify(kafkaTemplate).send(KafkaConfig.TODO_SYNC_TOPIC, "user123", todoSyncMessage);
        }
    }

    @Nested
    @DisplayName("Tests d'envoi de notifications")
    class NotificationMessageTests {

        @Test
        @DisplayName("Envoi réussi d'un message de notification")
        void sendNotificationMessage_Success() {
            // Given
            when(kafkaTemplate.send(eq(KafkaConfig.NOTIFICATION_TOPIC), eq("user123"), eq(notificationMessage)))
                    .thenReturn(mockFuture);
            when(mockFuture.whenComplete(any())).thenReturn(mockFuture);

            // When
            kafkaMessageService.sendNotificationMessage(notificationMessage);

            // Then
            verify(kafkaTemplate).send(KafkaConfig.NOTIFICATION_TOPIC, "user123", notificationMessage);
            verify(mockFuture).whenComplete(any());
        }

        @Test
        @DisplayName("Envoi de notifications avec tous les types")
        void sendNotificationMessage_AllTypes() {
            // Given
            NotificationMessage.NotificationType[] types = NotificationMessage.NotificationType.values();
            when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(mockFuture);
            when(mockFuture.whenComplete(any())).thenReturn(mockFuture);

            // When
            for (NotificationMessage.NotificationType type : types) {
                NotificationMessage notification = NotificationMessage.create(
                        "user123",
                        type,
                        "Test " + type.name(),
                        "Testing " + type.name()
                );
                kafkaMessageService.sendNotificationMessage(notification);
            }

            // Then
            verify(kafkaTemplate, times(types.length)).send(
                    eq(KafkaConfig.NOTIFICATION_TOPIC),
                    eq("user123"),
                    any(NotificationMessage.class)
            );
        }

        @Test
        @DisplayName("Envoi de notification avec userId null")
        void sendNotificationMessage_NullUserId() {
            // Given
            notificationMessage.setUserId(null);
            when(kafkaTemplate.send(eq(KafkaConfig.NOTIFICATION_TOPIC), isNull(), eq(notificationMessage)))
                    .thenReturn(mockFuture);
            when(mockFuture.whenComplete(any())).thenReturn(mockFuture);

            // When
            kafkaMessageService.sendNotificationMessage(notificationMessage);

            // Then
            verify(kafkaTemplate).send(KafkaConfig.NOTIFICATION_TOPIC, null, notificationMessage);
        }

        @Test
        @DisplayName("Envoi de notification avec données supplémentaires")
        void sendNotificationMessage_WithData() {
            // Given
            Object customData = new CustomNotificationData("extra", "information");
            notificationMessage.setData(customData);
            when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(mockFuture);
            when(mockFuture.whenComplete(any())).thenReturn(mockFuture);

            // When
            kafkaMessageService.sendNotificationMessage(notificationMessage);

            // Then
            verify(kafkaTemplate).send(
                    eq(KafkaConfig.NOTIFICATION_TOPIC),
                    eq("user123"),
                    argThat(msg -> {
                        NotificationMessage notification = (NotificationMessage) msg;
                        return notification.getData() != null &&
                               notification.getData().equals(customData);
                    })
            );
        }
    }

    @Nested
    @DisplayName("Tests d'intégration et robustesse")
    class IntegrationTests {

        @Test
        @DisplayName("Envoi de messages vers tous les topics")
        void sendToAllTopics() {
            // Given
            when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(mockFuture);
            when(mockFuture.whenComplete(any())).thenReturn(mockFuture);

            // When
            kafkaMessageService.sendPdfProcessingMessage(pdfMessage);
            kafkaMessageService.sendTodoSyncMessage(todoSyncMessage);
            kafkaMessageService.sendNotificationMessage(notificationMessage);

            // Then
            verify(kafkaTemplate).send(KafkaConfig.PDF_PROCESSING_TOPIC, "task-123", pdfMessage);
            verify(kafkaTemplate).send(KafkaConfig.TODO_SYNC_TOPIC, "user123", todoSyncMessage);
            verify(kafkaTemplate).send(KafkaConfig.NOTIFICATION_TOPIC, "user123", notificationMessage);
        }

        @Test
        @DisplayName("Performance avec envois multiples")
        void performance_MultipleMessages() {
            // Given
            when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(mockFuture);
            when(mockFuture.whenComplete(any())).thenReturn(mockFuture);

            int messageCount = 1000;

            // When
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < messageCount; i++) {
                NotificationMessage message = NotificationMessage.create(
                        "user" + i,
                        NotificationMessage.NotificationType.TODO_CREATED,
                        "Message " + i,
                        "Content " + i
                );
                kafkaMessageService.sendNotificationMessage(message);
            }
            long endTime = System.currentTimeMillis();

            // Then
            verify(kafkaTemplate, times(messageCount)).send(
                    eq(KafkaConfig.NOTIFICATION_TOPIC),
                    anyString(),
                    any(NotificationMessage.class)
            );

            long duration = endTime - startTime;
            System.out.println("Sent " + messageCount + " messages in " + duration + "ms");
        }

        @Test
        @DisplayName("Résilience avec Kafka template défaillant")
        void resilience_KafkaTemplateFailure() {
            // Given
            when(kafkaTemplate.send(anyString(), anyString(), any()))
                    .thenThrow(new RuntimeException("Kafka broker unavailable"))
                    .thenReturn(mockFuture); // Recovery sur second essai

            when(mockFuture.whenComplete(any())).thenReturn(mockFuture);

            // When - Premier envoi échoue, deuxième réussit
            try {
                kafkaMessageService.sendNotificationMessage(notificationMessage);
            } catch (Exception e) {
                // Expected
            }

            kafkaMessageService.sendNotificationMessage(notificationMessage);

            // Then
            verify(kafkaTemplate, times(2)).send(
                    eq(KafkaConfig.NOTIFICATION_TOPIC),
                    eq("user123"),
                    any(NotificationMessage.class)
            );
        }

        @Test
        @DisplayName("Validation des clés de partitionnement")
        void validatePartitioningKeys() {
            // Given
            when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(mockFuture);
            when(mockFuture.whenComplete(any())).thenReturn(mockFuture);

            // When
            kafkaMessageService.sendPdfProcessingMessage(pdfMessage);
            kafkaMessageService.sendTodoSyncMessage(todoSyncMessage);
            kafkaMessageService.sendNotificationMessage(notificationMessage);

            // Then - Vérifier que les bonnes clés sont utilisées pour le partitionnement
            verify(kafkaTemplate).send(KafkaConfig.PDF_PROCESSING_TOPIC, "task-123", pdfMessage); // taskId comme clé
            verify(kafkaTemplate).send(KafkaConfig.TODO_SYNC_TOPIC, "user123", todoSyncMessage);   // userId comme clé
            verify(kafkaTemplate).send(KafkaConfig.NOTIFICATION_TOPIC, "user123", notificationMessage); // userId comme clé
        }

        @Test
        @DisplayName("Gestion des messages avec timestamps")
        void handleMessagesWithTimestamps() {
            // Given
            LocalDateTime now = LocalDateTime.now();
            pdfMessage.setTimestamp(now);
            todoSyncMessage.setTimestamp(now);
            notificationMessage.setTimestamp(now);

            when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(mockFuture);
            when(mockFuture.whenComplete(any())).thenReturn(mockFuture);

            // When
            kafkaMessageService.sendPdfProcessingMessage(pdfMessage);
            kafkaMessageService.sendTodoSyncMessage(todoSyncMessage);
            kafkaMessageService.sendNotificationMessage(notificationMessage);

            // Then
            verify(kafkaTemplate, times(3)).send(anyString(), anyString(), any());
        }
    }

    // Classe utilitaire pour les tests
    private static class CustomNotificationData {
        private final String field1;
        private final String field2;

        public CustomNotificationData(String field1, String field2) {
            this.field1 = field1;
            this.field2 = field2;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CustomNotificationData that = (CustomNotificationData) o;
            return java.util.Objects.equals(field1, that.field1) &&
                   java.util.Objects.equals(field2, that.field2);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(field1, field2);
        }
    }
}
