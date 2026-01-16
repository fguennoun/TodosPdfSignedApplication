package be.cm.todoapplication.service;

import be.cm.todoapplication.dto.messaging.NotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendNotificationToUser(String userId, NotificationMessage notification) {
        log.info("Sending WebSocket notification to user {}: {}", userId, notification.getTitle());
        try {
            messagingTemplate.convertAndSendToUser(
                    userId,
                    "/queue/notifications",
                    notification
            );
            log.debug("WebSocket notification sent successfully to user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to send WebSocket notification to user: {}", userId, e);
        }
    }

    public void sendNotificationToTopic(String topic, NotificationMessage notification) {
        log.info("Sending WebSocket notification to topic {}: {}", topic, notification.getTitle());
        try {
            messagingTemplate.convertAndSend(
                    "/topic/" + topic,
                    notification
            );
            log.debug("WebSocket notification sent successfully to topic: {}", topic);
        } catch (Exception e) {
            log.error("Failed to send WebSocket notification to topic: {}", topic, e);
        }
    }

    public void sendPdfProcessingUpdate(String userId, String taskId, String status, String message) {
        NotificationMessage notification = NotificationMessage.create(
                userId,
                NotificationMessage.NotificationType.PDF_PROCESSING_COMPLETED,
                "PDF Processing Update",
                message,
                new PdfProcessingUpdateData(taskId, status)
        );
        sendNotificationToUser(userId, notification);
    }

    public void sendTodoSyncUpdate(String userId, String batchId, int processed, int total) {
        NotificationMessage notification = NotificationMessage.create(
                userId,
                NotificationMessage.NotificationType.SYNC_COMPLETED,
                "Todo Sync Progress",
                String.format("Synchronized %d/%d todos", processed, total),
                new TodoSyncUpdateData(batchId, processed, total)
        );
        sendNotificationToUser(userId, notification);
    }

    // Data classes for structured notification payloads
    public record PdfProcessingUpdateData(String taskId, String status) {}
    public record TodoSyncUpdateData(String batchId, int processed, int total) {}
}
