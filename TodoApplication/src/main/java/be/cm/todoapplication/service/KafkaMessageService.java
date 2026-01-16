package be.cm.todoapplication.service;

import be.cm.todoapplication.config.KafkaConfig;
import be.cm.todoapplication.dto.messaging.NotificationMessage;
import be.cm.todoapplication.dto.messaging.PdfProcessingMessage;
import be.cm.todoapplication.dto.messaging.TodoSyncMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaMessageService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendPdfProcessingMessage(PdfProcessingMessage message) {
        log.info("Sending PDF processing message: {}", message);
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                KafkaConfig.PDF_PROCESSING_TOPIC,
                message.getTaskId(),
                message
        );

        future.whenComplete((result, exception) -> {
            if (exception == null) {
                log.debug("PDF processing message sent successfully: {}", message.getTaskId());
            } else {
                log.error("Failed to send PDF processing message: {}", message.getTaskId(), exception);
            }
        });
    }

    public void sendTodoSyncMessage(TodoSyncMessage message) {
        log.info("Sending todo sync message: {}", message);
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                KafkaConfig.TODO_SYNC_TOPIC,
                message.getUserId(),
                message
        );

        future.whenComplete((result, exception) -> {
            if (exception == null) {
                log.debug("Todo sync message sent successfully for user: {}", message.getUserId());
            } else {
                log.error("Failed to send todo sync message for user: {}", message.getUserId(), exception);
            }
        });
    }

    public void sendNotificationMessage(NotificationMessage message) {
        log.info("Sending notification message: {}", message);
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                KafkaConfig.NOTIFICATION_TOPIC,
                message.getUserId(),
                message
        );

        future.whenComplete((result, exception) -> {
            if (exception == null) {
                log.debug("Notification message sent successfully for user: {}", message.getUserId());
            } else {
                log.error("Failed to send notification message for user: {}", message.getUserId(), exception);
            }
        });
    }
}
