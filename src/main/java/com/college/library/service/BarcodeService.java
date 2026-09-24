package com.college.library.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class BarcodeService {

    /**
     * Generates a 1D Code 128 barcode image as a byte array (PNG format).
     * @param text The string to encode (e.g. "EECIT001")
     * @param width Image width in pixels
     * @param height Image height in pixels
     * @return PNG image bytes
     */
    public byte[] generateCode128Barcode(String text, int width, int height) throws IOException {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Barcode text cannot be empty");
        }

        Code128Writer writer = new Code128Writer();
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.MARGIN, 25); // Generous quiet zone for webcam scan detection

        BitMatrix bitMatrix = writer.encode(text.trim(), BarcodeFormat.CODE_128, width, height, hints);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
        return outputStream.toByteArray();
    }

    /**
     * Generates a high-contrast 2D QR Code image as a byte array (PNG format).
     * Extremely resilient for scanning directly off phone screens via laptop webcams.
     * @param text The string to encode (e.g. "EECIT001" or "978-1-032-78600-1")
     * @param width Image width in pixels
     * @param height Image height in pixels
     * @return PNG image bytes
     */
    public byte[] generateQRCode(String text, int width, int height) throws IOException {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("QR Code text cannot be empty");
        }

        com.google.zxing.qrcode.QRCodeWriter writer = new com.google.zxing.qrcode.QRCodeWriter();
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 2);
        hints.put(EncodeHintType.ERROR_CORRECTION, com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.M);

        try {
            BitMatrix bitMatrix = writer.encode(text.trim(), BarcodeFormat.QR_CODE, width, height, hints);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            return outputStream.toByteArray();
        } catch (com.google.zxing.WriterException e) {
            throw new IOException("Failed to encode QR code: " + e.getMessage(), e);
        }
    }
}
