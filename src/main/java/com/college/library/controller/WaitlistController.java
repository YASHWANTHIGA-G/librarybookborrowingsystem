package com.college.library.controller;

import com.college.library.dto.WaitlistRequest;
import com.college.library.dto.WaitlistResponse;
import com.college.library.service.LibraryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/waitlist")
@CrossOrigin(origins = "*")
public class WaitlistController {

    private final LibraryService libraryService;

    public WaitlistController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @PostMapping("/join")
    public ResponseEntity<WaitlistResponse> joinWaitlist(@Valid @RequestBody WaitlistRequest request) {
        WaitlistResponse response = libraryService.joinWaitlist(request.getStudentId(), request.getBookId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<WaitlistResponse>> getActiveWaitlist() {
        return ResponseEntity.ok(libraryService.getActiveWaitlist());
    }

    @GetMapping("/all")
    public ResponseEntity<List<WaitlistResponse>> getAllWaitlist() {
        return ResponseEntity.ok(libraryService.getAllWaitlist());
    }

    @PostMapping("/{id}/notify")
    public ResponseEntity<WaitlistResponse> notifyStudent(@PathVariable Long id) {
        WaitlistResponse response = libraryService.notifyStudent(id);
        return ResponseEntity.ok(response);
    }
}
