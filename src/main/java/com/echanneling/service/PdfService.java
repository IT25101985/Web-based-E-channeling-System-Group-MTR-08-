package com.echanneling.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.echanneling.entity.Appointment;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class PdfService implements PDFGenerator {

    @Override
    public byte[] generateMedicalRecordPDF(com.echanneling.entity.MedicalRecord record) {
        // Simple logic hiding the complexity of PDF generation
        try {
            Document document = new Document();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, out);
            document.open();
            document.add(new Paragraph("CareLink | Medical record #" + record.getId()));
            document.add(new Paragraph("Patient: " + record.getPatient().getFullName()));
            document.add(new Paragraph("Diagnosis: " + record.getDiagnosis()));
            for (var p : record.getPrescriptions()) document.add(new Paragraph(p.getMedication() + " | " + p.getDosage() + " | " + p.getDuration()));
            document.close();
            return out.toByteArray();
        } catch (DocumentException ex) { throw new IllegalStateException("Could not create PDF", ex); }
    }

    public ByteArrayInputStream generateAppointmentSummary(Appointment appointment) {
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Add Title
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, BaseColor.DARK_GRAY);
            Paragraph title = new Paragraph("Web Based E-Channeling Summary", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(30);
            document.add(title);

            // Add Table
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setWidths(new int[]{1, 2});
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

            addTableRow(table, "Appointment ID", "#" + appointment.getId());
            addTableRow(table, "Patient Name", appointment.getPatient().getFullName());
            addTableRow(table, "Doctor Name", appointment.getDoctor().getName());
            addTableRow(table, "Department", appointment.getDoctor().getSpecialization());
            addTableRow(table, "Date & Time", appointment.getAppointmentDate().format(formatter));
            addTableRow(table, "Status", appointment.getStatus());

            document.add(table);
            document.close();
            
        } catch (DocumentException ex) {
            ex.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    public ByteArrayInputStream generateInvoice(com.echanneling.entity.Invoice invoice) {
        try {
            Document document = new Document();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, out);
            document.open();
            document.add(new Paragraph("CARELINK / INVOICE #" + invoice.getId(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22)));
            document.add(new Paragraph("Hospital appointment receipt - demonstration system"));
            document.add(Chunk.NEWLINE);
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            addTableRow(table, "Patient", invoice.getAppointment().getPatient().getFullName());
            addTableRow(table, "Doctor", invoice.getAppointment().getDoctor().getName());
            addTableRow(table, "Appointment", String.valueOf(invoice.getAppointment().getAppointmentDate()));
            addTableRow(table, "Amount (LKR)", String.format(java.util.Locale.US, "%,.2f", invoice.getAmount()));
            addTableRow(table, "Payment status", invoice.getStatus());
            addTableRow(table, "Method", invoice.getPaymentMethod() == null ? "Historical invoice" : invoice.getPaymentMethod());
            if (invoice.getCollectedBy() != null) {
                addTableRow(table, "Cash collected by", invoice.getCollectedBy());
                addTableRow(table, "Collected at", String.valueOf(invoice.getCollectedAt()));
            }
            addTableRow(table, "Issued", String.valueOf(invoice.getCreatedAt()));
            document.add(table);
            document.close();
            return new ByteArrayInputStream(out.toByteArray());
        } catch (DocumentException ex) { throw new IllegalStateException("Could not create invoice", ex); }
    }

    private void addTableRow(PdfPTable table, String header, String content) {
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        Font contentFont = FontFactory.getFont(FontFactory.HELVETICA, 12);

        PdfPCell hCell = new PdfPCell(new Phrase(header, headerFont));
        hCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        hCell.setPadding(8);
        
        PdfPCell cCell = new PdfPCell(new Phrase(content, contentFont));
        cCell.setPadding(8);

        table.addCell(hCell);
        table.addCell(cCell);
    }
}
