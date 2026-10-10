package com.echanneling.service;

import com.echanneling.entity.MedicalRecord;

/**
 * Interface demonstrating Abstraction.
 * It hides the complex logic of PDF generation.
 */
public interface PDFGenerator {
    byte[] generateMedicalRecordPDF(MedicalRecord record);
}
