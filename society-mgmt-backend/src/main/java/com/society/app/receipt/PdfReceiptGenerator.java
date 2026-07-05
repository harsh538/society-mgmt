package com.society.app.receipt;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.society.app.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * OpenPDF-backed receipt renderer. Generates the PDF in memory then uploads to R2.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PdfReceiptGenerator {

    private static final DateTimeFormatter IST_FMT = DateTimeFormatter
            .ofPattern("dd MMM yyyy, HH:mm 'IST'")
            .withZone(ZoneId.of("Asia/Kolkata"));

    private final FileStorageService fileStorageService;

    public String generateReceipt(ReceiptPdfData data, String subfolder) {
        String filename = UUID.randomUUID() + ".pdf";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        Document doc = new Document();
        try {
            PdfWriter.getInstance(doc, baos);
            doc.open();

            // Title
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.BLACK);
            Paragraph title = new Paragraph(
                    safe(data.societyName()) + " — Payment Receipt", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(18f);
            doc.add(title);

            // Receipt number
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
            Paragraph rn = new Paragraph("Receipt No: " + data.receiptNumber(), boldFont);
            rn.setSpacingAfter(12f);
            doc.add(rn);

            // 2-col table
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.2f, 2f});

            addRow(table, "Unit", data.unitNumber());
            addRow(table, "Member", data.memberName());
            addRow(table, "Amount", formatRupees(data.amount()));
            addRow(table, "Payment Method", data.paymentMethod());
            addRow(table, "UTR / Reference",
                    data.utrReference() == null || data.utrReference().isBlank()
                            ? "—" : data.utrReference());
            addRow(table, "Payment Date",
                    data.paidAt() != null ? IST_FMT.format(data.paidAt()) : "—");
            addRow(table, "Issued At", IST_FMT.format(data.issuedAt()));

            doc.add(table);

            // Footer
            Font footerFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, Color.DARK_GRAY);
            Paragraph footer = new Paragraph("This is a computer-generated receipt.", footerFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(24f);
            doc.add(footer);

            doc.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate receipt PDF: " + e.getMessage(), e);
        }

        String key = fileStorageService.store(baos.toByteArray(), "application/pdf", subfolder, filename);
        log.info("Generated receipt PDF {} for {}", filename, data.receiptNumber());
        return key;
    }

    private static void addRow(PdfPTable table, String label, String value) {
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.BLACK);
        Font valFont = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.BLACK);

        PdfPCell l = new PdfPCell(new Phrase(label, labelFont));
        l.setPadding(6f);
        l.setBorderColor(Color.LIGHT_GRAY);
        table.addCell(l);

        PdfPCell v = new PdfPCell(new Phrase(value == null ? "" : value, valFont));
        v.setPadding(6f);
        v.setBorderColor(Color.LIGHT_GRAY);
        table.addCell(v);
    }

    private static String formatRupees(BigDecimal amount) {
        if (amount == null) return "Rs. 0.00";
        return "Rs. " + amount.setScale(2).toPlainString();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    /**
     * Immutable input bag for {@link #generateReceipt(ReceiptPdfData, String)}.
     */
    public record ReceiptPdfData(
            String receiptNumber,
            String unitNumber,
            String memberName,
            BigDecimal amount,
            String paymentMethod,
            String utrReference,
            OffsetDateTime paidAt,
            OffsetDateTime issuedAt,
            String societyName
    ) {}
}
