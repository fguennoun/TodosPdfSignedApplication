package be.cm.todoapplication.dto.messaging;

import java.time.LocalDateTime;

/**
 * DTO pour messages de notification sans Lombok
 */
public class NotificationMessage {

    private String userId;
    private NotificationType type;
    private String title;
    private String message;
    private Object data;
    private LocalDateTime timestamp;

    public enum NotificationType {
        TODO_CREATED, TODO_UPDATED, TODO_DELETED,
        PDF_PROCESSING_COMPLETED, PDF_PROCESSING_FAILED,
        SYNC_COMPLETED, SYNC_FAILED, SYSTEM_NOTIFICATION
    }

    // Constructeurs
    public NotificationMessage() {}

    public NotificationMessage(String userId, NotificationType type, String title,
                              String message, Object data, LocalDateTime timestamp) {
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.data = data;
        this.timestamp = timestamp;
    }

    public static NotificationMessage create(String userId, NotificationType type, String title, String message) {
        return new NotificationMessage(userId, type, title, message, null, LocalDateTime.now());
    }

    public static NotificationMessage create(String userId, NotificationType type, String title, String message, Object data) {
        return new NotificationMessage(userId, type, title, message, data, LocalDateTime.now());
    }

    // Getters et Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
