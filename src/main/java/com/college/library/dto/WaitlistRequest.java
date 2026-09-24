package com.college.library.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class WaitlistRequest {

    @NotBlank(message = "Student ID is required")
    private String studentId;

    @NotNull(message = "Book ID is required")
    private Long bookId;

    public WaitlistRequest() {
    }

    public WaitlistRequest(String studentId, Long bookId) {
        this.studentId = studentId;
        this.bookId = bookId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }
}
