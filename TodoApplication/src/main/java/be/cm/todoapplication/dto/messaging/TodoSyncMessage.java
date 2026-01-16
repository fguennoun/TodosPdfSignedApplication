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
        FETCH_FROM_JSONPLACEHOLDER,
        UPDATE_LOCAL_TODOS,
        COMPLETE_SYNC
    }

    public enum SyncStatus {
        STARTED,
        IN_PROGRESS,
        COMPLETED,
        FAILED
    }

    public static TodoSyncMessage createStart(String userId, String batchId) {
        return new TodoSyncMessage(userId, SyncAction.FETCH_FROM_JSONPLACEHOLDER,
                batchId, 0, 0, SyncStatus.STARTED, null, LocalDateTime.now());
    }
}
