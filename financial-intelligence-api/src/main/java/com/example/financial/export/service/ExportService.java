package com.example.financial.export.service;

import com.example.financial.entity.Transaction;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@Service
public class ExportService {

    public ByteArrayInputStream exportTransactionsToCsv(List<Transaction> transactions) {
        final CSVFormat format = CSVFormat.Builder.create()
                .setHeader("Date", "Description", "Category", "Amount", "Currency", "Risk Score")
                .build();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             CSVPrinter csvPrinter = new CSVPrinter(new PrintWriter(out), format)) {

            for (Transaction t : transactions) {
                csvPrinter.printRecord(
                        t.getTransactionDate().toString(),
                        t.getDescription(),
                        t.getCategory() != null ? t.getCategory().toString() : "",
                        t.getAmount().toString(),
                        "USD", // Default or fetch from somewhere else if needed
                        t.getFraudRiskScore() != null ? t.getFraudRiskScore().toString() : "N/A"
                );
            }

            csvPrinter.flush();
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate CSV", e);
        }
    }

    public ByteArrayInputStream exportTransactionsToPdf(List<Transaction> transactions) {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Title
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("Transaction Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Table
            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[] { 3, 5, 3, 2, 2, 2 }); // column widths

            String[] headers = {"Date", "Description", "Category", "Amount", "Curr", "Risk"};
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);

            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            Font rowFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            for (Transaction t : transactions) {
                table.addCell(new PdfPCell(new Phrase(t.getTransactionDate().toString(), rowFont)));
                table.addCell(new PdfPCell(new Phrase(t.getDescription(), rowFont)));
                table.addCell(new PdfPCell(new Phrase(t.getCategory() != null ? t.getCategory().toString() : "", rowFont)));
                table.addCell(new PdfPCell(new Phrase(t.getAmount().toString(), rowFont)));
                table.addCell(new PdfPCell(new Phrase("USD", rowFont)));
                
                String risk = t.getFraudRiskScore() != null ? String.format("%.2f", t.getFraudRiskScore()) : "N/A";
                table.addCell(new PdfPCell(new Phrase(risk, rowFont)));
            }

            document.add(table);
            document.close();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }

        return new ByteArrayInputStream(out.toByteArray());
    }
}
