package com.college.library.controller;

import com.college.library.entity.Student;
import com.college.library.service.BarcodeService;
import com.college.library.service.LibraryService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/barcode")
@CrossOrigin(origins = "*")
public class BarcodeController {

    private final BarcodeService barcodeService;
    private final LibraryService libraryService;

    public BarcodeController(BarcodeService barcodeService, LibraryService libraryService) {
        this.barcodeService = barcodeService;
        this.libraryService = libraryService;
    }

    /**
     * Generates a Code 128 1D Barcode image for a student ID.
     * Barcode strictly contains only the student ID (e.g. EECIT001).
     */
    @GetMapping(value = "/student/{studentId}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getStudentBarcode(
            @PathVariable String studentId,
            @RequestParam(defaultValue = "450") int width,
            @RequestParam(defaultValue = "150") int height) throws IOException {

        Student student = libraryService.getStudentByIdOrBarcode(studentId);
        String codeData = (student.getBarcode() != null && !student.getBarcode().isBlank())
                ? student.getBarcode()
                : student.getStudentId();

        byte[] barcodeImage = barcodeService.generateCode128Barcode(codeData, width, height);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.setCacheControl("max-age=3600");

        return ResponseEntity.ok()
                .headers(headers)
                .body(barcodeImage);
    }

    /**
     * Direct Code 128 1D Barcode generator for any text data.
     */
    @GetMapping(value = "/generate", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> generateBarcode(
            @RequestParam(defaultValue = "EECIT001") String data,
            @RequestParam(defaultValue = "450") int width,
            @RequestParam(defaultValue = "150") int height) throws IOException {

        byte[] barcodeImage = barcodeService.generateCode128Barcode(data, width, height);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.setCacheControl("max-age=3600");

        return ResponseEntity.ok()
                .headers(headers)
                .body(barcodeImage);
    }

    /**
     * Generates a 2D QR Code image for a student ID.
     */
    @GetMapping(value = "/student/{studentId}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getStudentQrCode(
            @PathVariable String studentId,
            @RequestParam(defaultValue = "350") int size) throws IOException {

        Student student = libraryService.getStudentByIdOrBarcode(studentId);
        String codeData = (student.getBarcode() != null && !student.getBarcode().isBlank())
                ? student.getBarcode()
                : student.getStudentId();

        byte[] qrImage = barcodeService.generateQRCode(codeData, size, size);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.setCacheControl("max-age=3600");

        return ResponseEntity.ok()
                .headers(headers)
                .body(qrImage);
    }

    /**
     * Direct QR Code generator for any text data (e.g. ISBNs, book IDs, student codes).
     */
    @GetMapping(value = "/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> generateQrCode(
            @RequestParam(defaultValue = "EECIT001") String data,
            @RequestParam(defaultValue = "350") int size) throws IOException {

        byte[] qrImage = barcodeService.generateQRCode(data, size, size);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.setCacheControl("max-age=3600");

        return ResponseEntity.ok()
                .headers(headers)
                .body(qrImage);
    }
}
