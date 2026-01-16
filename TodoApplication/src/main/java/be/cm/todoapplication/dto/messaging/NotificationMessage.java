package be.cm.todoapplication.dto.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage {
    private String userId;
    private NotificationType type;
    private String title;
    private String message;
    private Object data;
    private LocalDateTime timestamp;

    public enum NotificationType {
        TODO_CREATED,
        TODO_UPDATED,
        TODO_DELETED,
        PDF_PROCESSING_COMPLETED,
        PDF_PROCESSING_FAILED,
        SYNC_COMPLETED,
        SYNC_FAILED,
        SYSTEM_NOTIFICATION
    }

    public static NotificationMessage create(String userId, NotificationType type, String title, String message) {
        return new NotificationMessage(userId, type, title, message, null, LocalDateTime.now());
    }

    public static NotificationMessage create(String userId, NotificationType type, String title, String message, Object data) {
        return new NotificationMessage(userId, type, title, message, data, LocalDateTime.now());
    }
}
