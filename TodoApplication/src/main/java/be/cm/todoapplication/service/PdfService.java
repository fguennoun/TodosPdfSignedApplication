package be.cm.todoapplication.service;

import be.cm.todoapplication.dto.TodoDTO;
import be.cm.todoapplication.dto.UserDTO;
import be.cm.todoapplication.dto.messaging.NotificationMessage;
import be.cm.todoapplication.dto.messaging.PdfProcessingMessage;
import be.cm.todoapplication.model.Todo;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfService {

    private final KafkaMessageService kafkaMessageService;
    private final WebSocketNotificationService webSocketNotificationService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String PDF_STORAGE_PATH = "pdf-storage/";

    // ... existing methods ...

    /**
     * Génère un PDF pour un TodoDTO
     */
    public byte[] generateTodoPdfFromDTO(TodoDTO todo) throws DocumentException {
        log.info("Génération PDF pour TodoDTO ID: {}", todo.getId());

        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        PdfWriter.getInstance(document, out);
        document.open();

        Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
        Font normalFont = new Font(Font.FontFamily.HELVETICA, 12);

        // Titre
        Paragraph title = new Paragraph("Détails de la Tâche", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        // Informations de la tâche
        document.add(new Paragraph("ID: " + todo.getId(), headerFont));
        document.add(new Paragraph("Titre: " + todo.getTitle(), headerFont));
        document.add(new Paragraph("Statut: " + (todo.getCompleted() ? "Terminée" : "En cours"), normalFont));
        document.add(new Paragraph("Utilisateur: " + todo.getUsername(), normalFont));

        if (todo.getDescription() != null && !todo.getDescription().isEmpty()) {
            document.add(new Paragraph(" ", normalFont));
            document.add(new Paragraph("Description:", headerFont));
            document.add(new Paragraph(todo.getDescription(), normalFont));
        }

        // Informations d'audit
        document.add(new LineSeparator());
        document.add(new Paragraph(" ", normalFont));

        if (todo.getCreatedAt() != null) {
            document.add(new Paragraph("Créée le: " + todo.getCreatedAt().format(DATE_FORMATTER), normalFont));
        }
        if (todo.getCreatedBy() != null) {
            document.add(new Paragraph("Créée par: " + todo.getCreatedBy(), normalFont));
        }
        if (todo.getUpdatedAt() != null) {
            document.add(new Paragraph("Modifiée le: " + todo.getUpdatedAt().format(DATE_FORMATTER), normalFont));
        }
        if (todo.getUpdatedBy() != null) {
            document.add(new Paragraph("Modifiée par: " + todo.getUpdatedBy(), normalFont));
        }

        document.close();
        return out.toByteArray();
    }

    /**
     * Génère un PDF signé pour un TodoDTO
     */
    public byte[] generateTodoPdfWithSignatureFromDTO(TodoDTO todo, String signature) throws DocumentException {
        log.info("Génération PDF signé pour TodoDTO ID: {}", todo.getId());

        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        PdfWriter.getInstance(document, out);
        document.open();

        Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
        Font normalFont = new Font(Font.FontFamily.HELVETICA, 12);
        Font signatureFont = new Font(Font.FontFamily.HELVETICA, 14, Font.ITALIC, BaseColor.BLUE);

        // Titre avec mention "SIGNÉ"
        Paragraph title = new Paragraph("Tâche Signée Électroniquement", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        // Watermark de sécurité
        Paragraph watermark = new Paragraph("DOCUMENT SIGNÉ - " + java.time.LocalDateTime.now().format(DATE_FORMATTER),
                                          new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, BaseColor.GRAY));
        watermark.setAlignment(Element.ALIGN_RIGHT);
        document.add(watermark);

        document.add(new Paragraph(" ", normalFont));

        // Informations de la tâche
        document.add(new Paragraph("ID: " + todo.getId(), headerFont));
        document.add(new Paragraph("Titre: " + todo.getTitle(), headerFont));
        document.add(new Paragraph("Statut: " + (todo.getCompleted() ? "Terminée" : "En cours"), normalFont));
        document.add(new Paragraph("Utilisateur: " + todo.getUsername(), normalFont));

        if (todo.getDescription() != null && !todo.getDescription().isEmpty()) {
            document.add(new Paragraph(" ", normalFont));
            document.add(new Paragraph("Description:", headerFont));
            document.add(new Paragraph(todo.getDescription(), normalFont));
        }

        // Section signature
        document.add(new LineSeparator());
        document.add(new Paragraph(" ", normalFont));
        document.add(new Paragraph("Signature Électronique:", headerFont));
        document.add(new Paragraph(signature, signatureFont));
        document.add(new Paragraph("Signé le: " + java.time.LocalDateTime.now().format(DATE_FORMATTER), normalFont));

        // Informations d'audit
        document.add(new Paragraph(" ", normalFont));
        if (todo.getCreatedAt() != null) {
            document.add(new Paragraph("Créée le: " + todo.getCreatedAt().format(DATE_FORMATTER), normalFont));
        }
        if (todo.getCreatedBy() != null) {
            document.add(new Paragraph("Créée par: " + todo.getCreatedBy(), normalFont));
        }

        document.close();
        return out.toByteArray();
    }

    public byte[] generateUsersListPdf(List<UserDTO> users) throws DocumentException {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        PdfWriter.getInstance(document, out);
        document.open();

        Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        Font normalFont = new Font(Font.FontFamily.HELVETICA, 10);

        Paragraph title = new Paragraph("Liste des Utilisateurs", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setSpacingAfter(10f);

        String[] headers = {"ID", "Nom", "Username", "Email", "Téléphone"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(5);
            table.addCell(cell);
        }

        for (UserDTO user : users) {
            table.addCell(new Phrase(String.valueOf(user.getId()), normalFont));
            table.addCell(new Phrase(user.getName(), normalFont));
            table.addCell(new Phrase(user.getUsername(), normalFont));
            table.addCell(new Phrase(user.getEmail(), normalFont));
            table.addCell(new Phrase(user.getPhone(), normalFont));
        }

        document.add(table);
        document.close();

        return out.toByteArray();
    }

    public byte[] generateTodoPdf(Todo todo) throws DocumentException, IOException {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        PdfWriter writer = PdfWriter.getInstance(document, out);
        document.open();

        Font titleFont = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD);
        Font labelFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        Font normalFont = new Font(Font.FontFamily.HELVETICA, 12);

        Paragraph title = new Paragraph("Détails de la Tâche", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(30);
        document.add(title);

        document.add(new Paragraph("ID: " + todo.getId(), normalFont));
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Utilisateur: " + todo.getUsername(), normalFont));
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Titre: " + todo.getTitle(), normalFont));
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Statut: " + (todo.getCompleted() ? "Complétée" : "En cours"), normalFont));
        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));

        Paragraph signatureLabel = new Paragraph("Signature Électronique:", labelFont);
        signatureLabel.setSpacingBefore(50);
        document.add(signatureLabel);

        LineSeparator line = new LineSeparator();
        line.setLineWidth(1);
        line.setPercentage(50);
        document.add(new Chunk(line));

        document.close();

        return out.toByteArray();
    }

    public byte[] generateTodoPdfWithSignature(Todo todo, String signatureBase64)
            throws DocumentException, IOException {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        PdfWriter writer = PdfWriter.getInstance(document, out);
        document.open();

        Font titleFont = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD);
        Font labelFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        Font normalFont = new Font(Font.FontFamily.HELVETICA, 12);

        Paragraph title = new Paragraph("Détails de la Tâche", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(30);
        document.add(title);

        document.add(new Paragraph("ID: " + todo.getId(), normalFont));
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Utilisateur: " + todo.getUsername(), normalFont));
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Titre: " + todo.getTitle(), normalFont));
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Statut: " + (todo.getCompleted() ? "Complétée" : "En cours"), normalFont));
        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));

        if (signatureBase64 != null && !signatureBase64.isEmpty()) {
            String base64Image = signatureBase64.split(",")[1];
            byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Image);

            Image signature = Image.getInstance(imageBytes);
            signature.scaleToFit(200, 100);
            signature.setAlignment(Element.ALIGN_LEFT);

            Paragraph signatureLabel = new Paragraph("Signature Électronique:", labelFont);
            signatureLabel.setSpacingBefore(30);
            document.add(signatureLabel);
            document.add(signature);
        }

        document.close();

        return out.toByteArray();
    }

    /**
     * Méthode pour traitement asynchrone des PDFs volumineux
     */
    @Async("pdfProcessingExecutor")
    public CompletableFuture<String> processLargePdfAsync(String userId, String todoId, List<TodoDTO> todos) {
        String taskId = UUID.randomUUID().toString();
        String fileName = String.format("todos-bulk-%s.pdf", taskId);
        String filePath = PDF_STORAGE_PATH + fileName;

        try {
            // Envoyer message de début de traitement
            PdfProcessingMessage startMessage = PdfProcessingMessage.createPending(taskId, userId, todoId, fileName, filePath);
            kafkaMessageService.sendPdfProcessingMessage(startMessage);

            // Notification WebSocket de début
            webSocketNotificationService.sendPdfProcessingUpdate(userId, taskId, "PROCESSING", "PDF generation started...");

            log.info("Starting async PDF processing for task: {}", taskId);

            // Simulation du traitement lourd
            Thread.sleep(2000); // Simulate heavy processing

            // Génération du PDF
            byte[] pdfBytes = generateBulkTodosPdf(todos);

            // Sauvegarde du fichier
            Path pdfPath = Paths.get(filePath);
            Files.createDirectories(pdfPath.getParent());
            Files.write(pdfPath, pdfBytes);

            // Message de succès
            PdfProcessingMessage completedMessage = new PdfProcessingMessage(
                taskId, userId, todoId, fileName, filePath,
                PdfProcessingMessage.ProcessingStatus.COMPLETED, null,
                java.time.LocalDateTime.now()
            );
            kafkaMessageService.sendPdfProcessingMessage(completedMessage);

            // Notification WebSocket de succès
            webSocketNotificationService.sendPdfProcessingUpdate(userId, taskId, "COMPLETED", "PDF generated successfully!");

            // Notification générale
            NotificationMessage notification = NotificationMessage.create(
                userId,
                NotificationMessage.NotificationType.PDF_PROCESSING_COMPLETED,
                "PDF Ready",
                String.format("Your PDF with %d todos is ready for download", todos.size()),
                taskId
            );
            kafkaMessageService.sendNotificationMessage(notification);

            log.info("Async PDF processing completed for task: {}", taskId);
            return CompletableFuture.completedFuture(taskId);

        } catch (Exception e) {
            log.error("Error in async PDF processing for task: {}", taskId, e);

            // Message d'erreur
            PdfProcessingMessage failedMessage = new PdfProcessingMessage(
                taskId, userId, todoId, fileName, filePath,
                PdfProcessingMessage.ProcessingStatus.FAILED, e.getMessage(),
                java.time.LocalDateTime.now()
            );
            kafkaMessageService.sendPdfProcessingMessage(failedMessage);

            // Notification WebSocket d'erreur
            webSocketNotificationService.sendPdfProcessingUpdate(userId, taskId, "FAILED", "PDF generation failed: " + e.getMessage());

            return CompletableFuture.failedFuture(e);
        }
    }

    private byte[] generateBulkTodosPdf(List<TodoDTO> todos) throws DocumentException {
        log.info("Generating bulk PDF for {} todos", todos.size());

        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        PdfWriter.getInstance(document, out);
        document.open();

        Font titleFont = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD);
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
        Font normalFont = new Font(Font.FontFamily.HELVETICA, 12);

        // Titre principal
        Paragraph title = new Paragraph("Rapport des Tâches", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(30);
        document.add(title);

        // Table pour les todos
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1, 3, 2, 2});

        // En-têtes
        addTableCell(table, "ID", headerFont, BaseColor.GRAY);
        addTableCell(table, "Titre", headerFont, BaseColor.GRAY);
        addTableCell(table, "Statut", headerFont, BaseColor.GRAY);
        addTableCell(table, "Utilisateur", headerFont, BaseColor.GRAY);

        // Données
        for (TodoDTO todo : todos) {
            addTableCell(table, todo.getId().toString(), normalFont, BaseColor.WHITE);
            addTableCell(table, todo.getTitle(), normalFont, BaseColor.WHITE);
            addTableCell(table, todo.getCompleted() ? "Terminé" : "En cours", normalFont, BaseColor.WHITE);
            addTableCell(table, todo.getUserId().toString(), normalFont, BaseColor.WHITE);
        }

        document.add(table);

        // Résumé
        document.add(new Paragraph("\n"));
        Paragraph summary = new Paragraph("Résumé:", headerFont);
        document.add(summary);

        long completedCount = todos.stream().mapToLong(t -> t.getCompleted() ? 1 : 0).sum();
        document.add(new Paragraph(String.format("Total des tâches: %d", todos.size()), normalFont));
        document.add(new Paragraph(String.format("Tâches terminées: %d", completedCount), normalFont));
        document.add(new Paragraph(String.format("Tâches en cours: %d", todos.size() - completedCount), normalFont));

        document.close();
        return out.toByteArray();
    }

    private void addTableCell(PdfPTable table, String content, Font font, BaseColor backgroundColor) {
        PdfPCell cell = new PdfPCell(new Phrase(content, font));
        cell.setBackgroundColor(backgroundColor);
        cell.setPadding(8);
        table.addCell(cell);
    }
}
