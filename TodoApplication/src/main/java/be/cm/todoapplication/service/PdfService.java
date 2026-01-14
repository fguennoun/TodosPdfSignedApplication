package be.cm.todoapplication.service;

import be.cm.todoapplication.dto.UserDTO;
import be.cm.todoapplication.model.Todo;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PdfService {

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
}
