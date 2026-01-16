package be.cm.todoapplication.dto.messaging;

import java.time.LocalDateTime;

/**
 * DTO pour messages de synchronisation Todo sans Lombok
 */
public class TodoSyncMessage {

    private String userId;
    private SyncAction action;
    private String batchId;
    private int totalTodos;
    private int processedTodos;
    private SyncStatus status;
    private String errorMessage;
    private LocalDateTime timestamp;

    public enum SyncAction {
        FETCH_FROM_JSONPLACEHOLDER, UPDATE_LOCAL_TODOS, COMPLETE_SYNC
    }

    public enum SyncStatus {
        STARTED, IN_PROGRESS, COMPLETED, FAILED
    }

    // Constructeurs
    public TodoSyncMessage() {}

    public TodoSyncMessage(String userId, SyncAction action, String batchId, int totalTodos,
                          int processedTodos, SyncStatus status, String errorMessage,
                          LocalDateTime timestamp) {
        this.userId = userId;
        this.action = action;
        this.batchId = batchId;
        this.totalTodos = totalTodos;
        this.processedTodos = processedTodos;
        this.status = status;
        this.errorMessage = errorMessage;
        this.timestamp = timestamp;
    }

    public static TodoSyncMessage createStart(String userId, String batchId) {
        return new TodoSyncMessage(userId, SyncAction.FETCH_FROM_JSONPLACEHOLDER,
                batchId, 0, 0, SyncStatus.STARTED, null, LocalDateTime.now());
    }

    // Getters et Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public SyncAction getAction() { return action; }
    public void setAction(SyncAction action) { this.action = action; }

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }

    public int getTotalTodos() { return totalTodos; }
    public void setTotalTodos(int totalTodos) { this.totalTodos = totalTodos; }

    public int getProcessedTodos() { return processedTodos; }
    public void setProcessedTodos(int processedTodos) { this.processedTodos = processedTodos; }

    public SyncStatus getStatus() { return status; }
    public void setStatus(SyncStatus status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
