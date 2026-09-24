package com.college.library;

import com.college.library.service.BarcodeService;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class BarcodeServiceTest {

    @Test
    void testCode128BarcodeGeneration() throws IOException {
        BarcodeService service = new BarcodeService();
        byte[] imageBytes = service.generateCode128Barcode("EECIT001", 400, 120);

        assertNotNull(imageBytes);
        assertTrue(imageBytes.length > 100, "Barcode image byte array should contain PNG data");

        // Verify PNG magic header bytes: 0x89 'P' 'N' 'G'
        assertEquals((byte) 0x89, imageBytes[0]);
        assertEquals((byte) 'P', imageBytes[1]);
        assertEquals((byte) 'N', imageBytes[2]);
        assertEquals((byte) 'G', imageBytes[3]);
    }
}
