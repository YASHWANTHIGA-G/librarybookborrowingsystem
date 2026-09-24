package com.college.library.dto;

import com.college.library.entity.WaitlistReservation;
import com.college.library.entity.WaitlistStatus;

import java.time.LocalDate;

public class WaitlistResponse {

    private Long id;
    private String studentId;
    private String studentName;
    private String studentEmail;
    private Long bookId;
    private String bookTitle;
    private String bookAuthor;
    private LocalDate reservationDate;
    private WaitlistStatus status;
    private LocalDate notificationDate;
    private boolean emailSent;

    public WaitlistResponse() {
    }

    public static WaitlistResponse fromEntity(WaitlistReservation entity) {
        WaitlistResponse dto = new WaitlistResponse();
        dto.setId(entity.getId());
        dto.setStudentId(entity.getStudent().getStudentId());
        dto.setStudentName(entity.getStudent().getName());
        dto.setStudentEmail(entity.getStudent().getEmail());
        dto.setBookId(entity.getBook().getId());
        dto.setBookTitle(entity.getBook().getTitle());
        dto.setBookAuthor(entity.getBook().getAuthor());
        dto.setReservationDate(entity.getReservationDate());
        dto.setStatus(entity.getStatus());
        dto.setNotificationDate(entity.getNotificationDate());
        dto.setEmailSent(entity.isEmailSent());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
    }

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }

    public String getBookAuthor() {
        return bookAuthor;
    }

    public void setBookAuthor(String bookAuthor) {
        this.bookAuthor = bookAuthor;
    }

    public LocalDate getReservationDate() {
        return reservationDate;
    }

    public void setReservationDate(LocalDate reservationDate) {
        this.reservationDate = reservationDate;
    }

    public WaitlistStatus getStatus() {
        return status;
    }

    public void setStatus(WaitlistStatus status) {
        this.status = status;
    }

    public LocalDate getNotificationDate() {
        return notificationDate;
    }

    public void setNotificationDate(LocalDate notificationDate) {
        this.notificationDate = notificationDate;
    }

    public boolean isEmailSent() {
        return emailSent;
    }

    public void setEmailSent(boolean emailSent) {
        this.emailSent = emailSent;
    }
}
