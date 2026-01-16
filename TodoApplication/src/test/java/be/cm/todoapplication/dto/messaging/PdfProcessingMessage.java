package be.cm.todoapplication.dto.messaging;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * DTO pour messages PDF sans Lombok
 */
public class PdfProcessingMessage {

    private String taskId;
    private String userId;
    private String todoId;
    private String fileName;
    private String filePath;
    private ProcessingStatus status;
    private String errorMessage;
    private LocalDateTime timestamp;

    public enum ProcessingStatus {
        PENDING, PROCESSING, COMPLETED, FAILED
    }

    // Constructeurs
    public PdfProcessingMessage() {}

    public PdfProcessingMessage(String taskId, String userId, String todoId, String fileName,
                               String filePath, ProcessingStatus status, String errorMessage,
                               LocalDateTime timestamp) {
        this.taskId = taskId;
        this.userId = userId;
        this.todoId = todoId;
        this.fileName = fileName;
        this.filePath = filePath;
        this.status = status;
        this.errorMessage = errorMessage;
        this.timestamp = timestamp;
    }

    public static PdfProcessingMessage createPending(String taskId, String userId, String todoId,
                                                    String fileName, String filePath) {
        return new PdfProcessingMessage(taskId, userId, todoId, fileName, filePath,
                ProcessingStatus.PENDING, null, LocalDateTime.now());
    }

    // Getters et Setters
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTodoId() { return todoId; }
    public void setTodoId(String todoId) { this.todoId = todoId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public ProcessingStatus getStatus() { return status; }
    public void setStatus(ProcessingStatus status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PdfProcessingMessage that = (PdfProcessingMessage) o;
        return Objects.equals(taskId, that.taskId) &&
               Objects.equals(userId, that.userId) &&
               Objects.equals(status, that.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId, userId, status);
    }
}
