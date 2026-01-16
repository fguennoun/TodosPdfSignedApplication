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
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED
    }

    public static PdfProcessingMessage createPending(String taskId, String userId, String todoId, String fileName, String filePath) {
        return new PdfProcessingMessage(taskId, userId, todoId, fileName, filePath,
                ProcessingStatus.PENDING, null, LocalDateTime.now());
    }
}
