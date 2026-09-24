package com.college.library.controller;

import com.college.library.dto.IssueRequest;
import com.college.library.dto.TransactionResponse;
import com.college.library.service.LibraryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/borrow")
@CrossOrigin(origins = "*")
public class BorrowController {

    private final LibraryService libraryService;

    public BorrowController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    /**
     * Issues a book to a student.
     * Decrements available copies, computes 14-day due date, writes to MySQL, and sends email.
     */
    @PostMapping("/issue")
    public ResponseEntity<TransactionResponse> issueBook(@Valid @RequestBody IssueRequest request) {
        TransactionResponse response = libraryService.issueBook(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Returns an issued book.
     * Increments available copies, marks transaction RETURNED in MySQL.
     */
    @PostMapping("/return/{transactionId}")
    public ResponseEntity<TransactionResponse> returnBook(@PathVariable Long transactionId) {
        TransactionResponse response = libraryService.returnBook(transactionId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/active")
    public ResponseEntity<List<TransactionResponse>> getActiveTransactions() {
        return ResponseEntity.ok(libraryService.getActiveTransactions());
    }

    @GetMapping("/history")
    public ResponseEntity<List<TransactionResponse>> getAllTransactions() {
        return ResponseEntity.ok(libraryService.getAllTransactions());
    }
}
