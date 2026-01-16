package be.cm.todoapplication.controller;

import be.cm.todoapplication.dto.messaging.NotificationMessage;
import be.cm.todoapplication.service.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final WebSocketNotificationService webSocketNotificationService;

    /**
     * Gère l'abonnement aux notifications personnelles de l'utilisateur
     */
    @SubscribeMapping("/queue/notifications")
    public NotificationMessage subscribeToNotifications(Principal principal) {
        String username = principal.getName();
        log.info("User {} subscribed to personal notifications", username);

        // Envoyer un message de bienvenue
        return NotificationMessage.create(
            username,
            NotificationMessage.NotificationType.SYSTEM_NOTIFICATION,
            "Connection Established",
            "You are now connected to real-time notifications"
        );
    }

    /**
     * Gère l'abonnement aux mises à jour de traitement PDF
     */
    @SubscribeMapping("/queue/pdf-updates")
    public NotificationMessage subscribeToPdfUpdates(Principal principal) {
        String username = principal.getName();
        log.info("User {} subscribed to PDF processing updates", username);

        return NotificationMessage.create(
            username,
            NotificationMessage.NotificationType.SYSTEM_NOTIFICATION,
            "PDF Updates",
            "You will receive real-time PDF processing updates"
        );
    }

    /**
     * Gère l'abonnement aux mises à jour de synchronisation
     */
    @SubscribeMapping("/queue/sync-updates")
    public NotificationMessage subscribeToSyncUpdates(Principal principal) {
        String username = principal.getName();
        log.info("User {} subscribed to sync updates", username);

        return NotificationMessage.create(
            username,
            NotificationMessage.NotificationType.SYSTEM_NOTIFICATION,
            "Sync Updates",
            "You will receive real-time synchronization updates"
        );
    }

    /**
     * Gère les messages envoyés par les clients (ping/pong pour maintenir la connexion)
     */
    @MessageMapping("/ping")
    @SendToUser("/queue/notifications")
    public NotificationMessage handlePing(@Payload String message, Principal principal) {
        log.debug("Received ping from user: {}", principal.getName());

        return NotificationMessage.create(
            principal.getName(),
            NotificationMessage.NotificationType.SYSTEM_NOTIFICATION,
            "Pong",
            "Connection is active"
        );
    }

    /**
     * Permet aux utilisateurs de demander le statut de leurs tâches en cours
     */
    @MessageMapping("/status")
    @SendToUser("/queue/notifications")
    public NotificationMessage getTaskStatus(@Payload String taskId, Principal principal) {
        log.info("User {} requested status for task: {}", principal.getName(), taskId);

        // Ici, on pourrait interroger un service pour obtenir le statut réel de la tâche
        return NotificationMessage.create(
            principal.getName(),
            NotificationMessage.NotificationType.SYSTEM_NOTIFICATION,
            "Task Status",
            String.format("Status requested for task: %s", taskId),
            taskId
        );
    }
}
