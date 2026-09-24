package com.college.library.dto;

import com.college.library.entity.BorrowTransaction;
import com.college.library.entity.TransactionStatus;
import java.time.LocalDate;
import java.util.List;

public class TransactionResponse {

    private Long id;
    private Long studentDatabaseId;
    private String studentId;
    private String studentName;
    private String studentDepartment;
    private String studentSection;
    private Integer studentYear;
    private String studentEmail;

    private Long bookId;
    private String bookTitle;
    private String bookAuthor;
    private String bookIsbn;
    private Integer availableCopies;

    private LocalDate issueDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private TransactionStatus status;
    private Boolean emailSent;
    private List<WaitlistResponse> waitingStudents;

    public TransactionResponse() {
    }

    public static TransactionResponse fromEntity(BorrowTransaction tx) {
        TransactionResponse dto = new TransactionResponse();
        dto.setId(tx.getId());

        if (tx.getStudent() != null) {
            dto.setStudentDatabaseId(tx.getStudent().getId());
            dto.setStudentId(tx.getStudent().getStudentId());
            dto.setStudentName(tx.getStudent().getName());
            dto.setStudentDepartment(tx.getStudent().getDepartment());
            dto.setStudentSection(tx.getStudent().getSection());
            dto.setStudentYear(tx.getStudent().getYear());
            dto.setStudentEmail(tx.getStudent().getEmail());
        }

        if (tx.getBook() != null) {
            dto.setBookId(tx.getBook().getId());
            dto.setBookTitle(tx.getBook().getTitle());
            dto.setBookAuthor(tx.getBook().getAuthor());
            dto.setBookIsbn(tx.getBook().getIsbn());
            dto.setAvailableCopies(tx.getBook().getAvailableCopies());
        }

        dto.setIssueDate(tx.getIssueDate());
        dto.setDueDate(tx.getDueDate());
        dto.setReturnDate(tx.getReturnDate());
        dto.setStatus(tx.getStatus());
        dto.setEmailSent(tx.getEmailSent());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStudentDatabaseId() {
        return studentDatabaseId;
    }

    public void setStudentDatabaseId(Long studentDatabaseId) {
        this.studentDatabaseId = studentDatabaseId;
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

    public String getStudentDepartment() {
        return studentDepartment;
    }

    public void setStudentDepartment(String studentDepartment) {
        this.studentDepartment = studentDepartment;
    }

    public String getStudentSection() {
        return studentSection;
    }

    public void setStudentSection(String studentSection) {
        this.sectionFormat();
        this.studentSection = studentSection;
    }

    private void sectionFormat() {
    }

    public Integer getStudentYear() {
        return studentYear;
    }

    public void setStudentYear(Integer studentYear) {
        this.studentYear = studentYear;
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

    public String getBookIsbn() {
        return bookIsbn;
    }

    public void setBookIsbn(String bookIsbn) {
        this.bookIsbn = bookIsbn;
    }

    public Integer getAvailableCopies() {
        return availableCopies;
    }

    public void setAvailableCopies(Integer availableCopies) {
        this.availableCopies = availableCopies;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public Boolean getEmailSent() {
        return emailSent;
    }

    public void setEmailSent(Boolean emailSent) {
        this.emailSent = emailSent;
    }

    public List<WaitlistResponse> getWaitingStudents() {
        return waitingStudents;
    }

    public void setWaitingStudents(List<WaitlistResponse> waitingStudents) {
        this.waitingStudents = waitingStudents;
    }
}
