package be.cm.todoapplication.service.messaging;

import be.cm.todoapplication.config.KafkaConfig;
import be.cm.todoapplication.dto.messaging.NotificationMessage;
import be.cm.todoapplication.dto.messaging.PdfProcessingMessage;
import be.cm.todoapplication.dto.messaging.TodoSyncMessage;
import be.cm.todoapplication.service.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaMessageListener {

    private final WebSocketNotificationService webSocketNotificationService;

    @KafkaListener(topics = KafkaConfig.PDF_PROCESSING_TOPIC, groupId = "pdf-processing-group")
    public void handlePdfProcessingMessage(
            @Payload PdfProcessingMessage message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            Acknowledgment acknowledgment) {

        try {
            log.info("Processing PDF message: {} for user: {}", message.getTaskId(), message.getUserId());

            // Traiter le message selon le statut
            switch (message.getStatus()) {
                case PENDING:
                    log.info("PDF processing task {} is pending", message.getTaskId());
                    break;

                case PROCESSING:
                    log.info("PDF processing task {} is in progress", message.getTaskId());
                    webSocketNotificationService.sendPdfProcessingUpdate(
                        message.getUserId(),
                        message.getTaskId(),
                        "PROCESSING",
                        "PDF generation in progress..."
                    );
                    break;

                case COMPLETED:
                    log.info("PDF processing task {} completed successfully", message.getTaskId());
                    // Envoyer notification de succès via WebSocket si pas déjà fait
                    break;

                case FAILED:
                    log.error("PDF processing task {} failed: {}", message.getTaskId(), message.getErrorMessage());
                    // Traitement des erreurs (retry, notification admin, etc.)
                    break;
            }

            // Confirmer le traitement
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing PDF message: {}", message.getTaskId(), e);
            // Ne pas acknowledger en cas d'erreur pour retry
        }
    }

    @KafkaListener(topics = KafkaConfig.TODO_SYNC_TOPIC, groupId = "todo-sync-group")
    public void handleTodoSyncMessage(
            @Payload TodoSyncMessage message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            Acknowledgment acknowledgment) {

        try {
            log.info("Processing todo sync message: {} for user: {}", message.getBatchId(), message.getUserId());

            // Traiter le message selon l'action et le statut
            switch (message.getStatus()) {
                case STARTED:
                    log.info("Todo sync batch {} started for user: {}", message.getBatchId(), message.getUserId());
                    break;

                case IN_PROGRESS:
                    log.info("Todo sync batch {} in progress: {}/{}",
                            message.getBatchId(), message.getProcessedTodos(), message.getTotalTodos());
                    break;

                case COMPLETED:
                    log.info("Todo sync batch {} completed successfully for user: {}",
                            message.getBatchId(), message.getUserId());
                    // Peut déclencher des actions post-synchronisation
                    break;

                case FAILED:
                    log.error("Todo sync batch {} failed for user: {}: {}",
                            message.getBatchId(), message.getUserId(), message.getErrorMessage());
                    // Traitement des erreurs de synchronisation
                    break;
            }

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing todo sync message: {}", message.getBatchId(), e);
        }
    }

    @KafkaListener(topics = KafkaConfig.NOTIFICATION_TOPIC, groupId = "notification-group")
    public void handleNotificationMessage(
            @Payload NotificationMessage message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            Acknowledgment acknowledgment) {

        try {
            log.info("Processing notification message for user: {} - {}", message.getUserId(), message.getTitle());

            // Envoyer la notification via WebSocket
            webSocketNotificationService.sendNotificationToUser(message.getUserId(), message);

            // Selon le type de notification, peut déclencher d'autres actions
            switch (message.getType()) {
                case PDF_PROCESSING_COMPLETED:
                    log.info("PDF processing notification sent to user: {}", message.getUserId());
                    break;

                case SYNC_COMPLETED:
                    log.info("Sync completion notification sent to user: {}", message.getUserId());
                    break;

                case SYSTEM_NOTIFICATION:
                    log.info("System notification sent to user: {}", message.getUserId());
                    // Peut également envoyer à tous les utilisateurs connectés
                    webSocketNotificationService.sendNotificationToTopic("system", message);
                    break;

                default:
                    log.info("General notification sent to user: {}", message.getUserId());
            }

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing notification message for user: {}", message.getUserId(), e);
        }
    }
}
