package com.college.library.controller;

import com.college.library.dto.TransactionResponse;
import com.college.library.entity.Student;
import com.college.library.service.LibraryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/students")
@CrossOrigin(origins = "*")
public class StudentController {

    private final LibraryService libraryService;

    public StudentController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    /**
     * Look up student details by Student ID or Barcode (e.g. EECIT001).
     * Fetches real entity from MySQL.
     */
    @GetMapping("/{studentId}")
    public ResponseEntity<Student> getStudent(@PathVariable String studentId) {
        Student student = libraryService.getStudentByIdOrBarcode(studentId);
        return ResponseEntity.ok(student);
    }

    @GetMapping
    public ResponseEntity<List<Student>> getAllStudents() {
        return ResponseEntity.ok(libraryService.getAllStudents());
    }

    @GetMapping("/{studentId}/transactions")
    public ResponseEntity<List<TransactionResponse>> getStudentTransactions(@PathVariable String studentId) {
        return ResponseEntity.ok(libraryService.getStudentTransactions(studentId));
    }

    @PostMapping
    public ResponseEntity<Student> createOrUpdateStudent(@RequestBody Student student) {
        return ResponseEntity.ok(libraryService.saveStudent(student));
    }
}
