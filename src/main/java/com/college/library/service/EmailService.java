package com.college.library.service;

import com.college.library.entity.Book;
import com.college.library.entity.Student;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy");

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String smtpUsername;

    @Value("${library.mail.from:noreply.library@college.edu}")
    private String mailFrom;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public synchronized void updateCredentials(String username, String password) {
        this.smtpUsername = (username != null) ? username.trim() : "";
        if (mailSender instanceof org.springframework.mail.javamail.JavaMailSenderImpl impl) {
            impl.setHost("smtp.gmail.com");
            impl.setPort(587);
            impl.setUsername(this.smtpUsername);
            impl.setPassword((password != null) ? password.trim().replace(" ", "") : "");
            java.util.Properties props = impl.getJavaMailProperties();
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
            props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");
            props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
            props.put("mail.smtp.localhost", "localhost");
            props.put("mail.smtp.connectiontimeout", "15000");
            props.put("mail.smtp.timeout", "15000");
            props.put("mail.smtp.writetimeout", "15000");
        }
    }

    public String getSmtpUsername() {
        return smtpUsername;
    }

    /**
     * Sends email upon successful book issuance.
     */
    public boolean sendBookIssuedEmail(Student student, Book book, LocalDate issueDate, LocalDate dueDate) {
        if (student == null || student.getEmail() == null || student.getEmail().isBlank()) {
            log.warn("Cannot send email: Student or student email is missing.");
            return false;
        }

        String recipientEmail = student.getEmail().trim();
        String formattedIssueDate = issueDate.format(DATE_FORMATTER);
        String formattedDueDate = dueDate.format(DATE_FORMATTER);
        String subject = "Book Issued Successfully - College Library";

        String textContent = String.format(
                "Book Issued Successfully\n\n" +
                "Student: %s\n" +
                "Book: %s\n" +
                "Issue Date: %s\n" +
                "Return Date: %s\n\n" +
                "Please return the book before the return date.",
                student.getName(),
                book.getTitle(),
                formattedIssueDate,
                formattedDueDate
        );

        String htmlContent = buildIssuedHtml(student, book, formattedIssueDate, formattedDueDate);

        return dispatchEmail(recipientEmail, student.getName(), subject, "ISSUED", textContent, htmlContent);
    }

    /**
     * Sends return receipt email to student upon returning a book.
     */
    public boolean sendBookReturnedEmail(Student student, Book book, LocalDate returnDate) {
        if (student == null || student.getEmail() == null || student.getEmail().isBlank()) {
            return false;
        }

        String recipientEmail = student.getEmail().trim();
        String formattedReturnDate = returnDate.format(DATE_FORMATTER);
        String subject = "Book Returned Successfully - College Library";

        String textContent = String.format(
                "Book Returned Successfully\n\n" +
                "Student: %s\n" +
                "Book: %s\n" +
                "Returned On: %s\n\n" +
                "Status: Completed (No Overdue Fines)\n" +
                "Thank you for returning the book on time to the college library.",
                student.getName(),
                book.getTitle(),
                formattedReturnDate
        );

        String htmlContent = buildReturnedHtml(student, book, formattedReturnDate);

        return dispatchEmail(recipientEmail, student.getName(), subject, "RETURNED", textContent, htmlContent);
    }

    /**
     * Sends waitlist confirmation email when student reserves an out-of-stock book.
     */
    public boolean sendWaitlistConfirmationEmail(Student student, Book book) {
        if (student == null || student.getEmail() == null || student.getEmail().isBlank()) {
            return false;
        }

        String recipientEmail = student.getEmail().trim();
        String subject = "Waitlist Confirmation: " + book.getTitle() + " - College Library";

        String textContent = String.format(
                "Waitlist Priority Confirmation\n\n" +
                "Student: %s\n" +
                "Reserved Book: %s by %s\n" +
                "Date Reserved: %s\n\n" +
                "This book is currently fully borrowed. You have been placed on the priority waitlist.\n" +
                "We will automatically send you an email alert the moment another student returns this book.",
                student.getName(),
                book.getTitle(),
                book.getAuthor(),
                LocalDate.now().format(DATE_FORMATTER)
        );

        String htmlContent = buildWaitlistJoinedHtml(student, book);

        return dispatchEmail(recipientEmail, student.getName(), subject, "WAITLIST_JOINED", textContent, htmlContent);
    }

    /**
     * Sends notification email to student informing them to collect their reserved book from the library.
     */
    public boolean sendBookAvailableEmail(Student student, Book book) {
        if (student == null || student.getEmail() == null || student.getEmail().isBlank()) {
            return false;
        }

        String recipientEmail = student.getEmail().trim();
        String subject = "Reserved Book Ready for Collection: " + book.getTitle() + " - College Library";

        String textContent = String.format(
                "Reserved Book Ready for Collection\n\n" +
                "Dear %s,\n\n" +
                "Your reserved book \"%s\" by %s is now available at the library.\n\n" +
                "Please visit the college library circulation desk to collect your book.\n\n" +
                "Book Details:\n" +
                "- Title: %s\n" +
                "- Author: %s\n" +
                "- Pickup Location: Central Library Circulation Desk\n\n" +
                "Best regards,\n" +
                "College Library Circulation Desk",
                student.getName(),
                book.getTitle(),
                book.getAuthor(),
                book.getTitle(),
                book.getAuthor()
        );

        String htmlContent = buildBookAvailableHtml(student, book);

        return dispatchEmail(recipientEmail, student.getName(), subject, "BOOK_AVAILABLE", textContent, htmlContent);
    }

    private boolean dispatchEmail(String recipient, String studentName, String subject, String type, String textContent, String htmlContent) {
        boolean sentViaSmtp = false;

        if (smtpUsername != null && !smtpUsername.isBlank()) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(smtpUsername, "College Library Management System");
                helper.setTo(recipient);

                // Ensure the library administrator/sender account also receives a copy of every issued book notice
                if (smtpUsername != null && !smtpUsername.isBlank() && !smtpUsername.equalsIgnoreCase(recipient)) {
                    try {
                        helper.setBcc(smtpUsername);
                    } catch (Exception ignored) {}
                }

                helper.setSubject(subject);
                helper.setText(textContent, htmlContent);

                mailSender.send(message);
                sentViaSmtp = true;
                log.info(">>> REAL EMAIL DELIVERED via SMTP to {} [Subject: {}]", recipient, subject);
            } catch (Exception ex) {
                log.error(">>> REAL EMAIL SMTP DELIVERY FAILED to {}: {}", recipient, ex.getMessage());
            }
        } else {
            log.warn(">>> SMTP SENDER NOT CONFIGURED: Cannot send real email to {}. Please configure your Gmail credentials in the top navigation bar.", recipient);
        }

        return sentViaSmtp;
    }

    /**
     * Sends a live test email to verify SMTP connection and delivery to real student inboxes.
     */
    public java.util.Map<String, Object> sendTestEmail(String testRecipient) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        if (!isSmtpConfigured()) {
            result.put("success", false);
            result.put("message", "SMTP credentials not configured. Please enter your Gmail address and 16-character App Password.");
            return result;
        }

        String target = (testRecipient != null && !testRecipient.isBlank()) ? testRecipient.trim() : "310625205283@eec.srmrmp.edu.in";

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(smtpUsername, "College Library Management System");
            helper.setTo(target);
            helper.setSubject("🧪 College Library System - Real Email Test");

            String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background:#0f172a; color:#f8fafc; padding:24px; }
                        .box { max-width:540px; margin:0 auto; background:#1e293b; border-radius:14px; padding:28px; border:1px solid rgba(255,255,255,0.1); }
                        .badge { background:rgba(16,185,129,0.2); color:#34d399; font-size:12px; font-weight:700; padding:4px 10px; border-radius:12px; display:inline-block; margin-bottom:12px; }
                        h2 { color:#fff; margin-top:0; }
                        p { color:#cbd5e1; line-height:1.6; font-size:14px; }
                        .details { background:#0f172a; border-radius:8px; padding:14px; font-family:monospace; font-size:13px; color:#38bdf8; margin:16px 0; }
                    </style>
                </head>
                <body>
                    <div class="box">
                        <div class="badge">LIVE SMTP DELIVERY CONFIRMED</div>
                        <h2>College Library Email Dispatcher</h2>
                        <p>Hello! This is a live verification email proving that the <strong>College Library Management System</strong> is successfully sending real emails across the internet.</p>
                        <div class="details">
                            Sender: %s<br>
                            Recipient: %s<br>
                            Status: Verified Real-Time Transmission
                        </div>
                        <p>Real-time notifications for book loans, returns, and priority waitlist alerts will be delivered to student inboxes.</p>
                        <p style="font-size:12px; color:#64748b; margin-top:24px; border-top:1px solid rgba(255,255,255,0.08); padding-top:12px;">Easwari Engineering College &bull; SRM RMP Campus Library</p>
                    </div>
                </body>
                </html>
            """.formatted(smtpUsername, target);

            helper.setText("Real email delivery test successful! Sent to " + target, html);
            mailSender.send(message);

            log.info(">>> TEST EMAIL DELIVERED SUCCESSFULLY to {} via SMTP", target);
            result.put("success", true);
            result.put("message", "Real test email delivered to " + target + "! Check your inbox (or Spam folder).");
        } catch (Exception ex) {
            log.error(">>> TEST EMAIL FAILED to {}: {}", target, ex.getMessage());
            result.put("success", false);
            result.put("message", "SMTP Delivery Failed: " + ex.getMessage());
        }
        return result;
    }

    private String buildIssuedHtml(Student student, Book book, String issueDate, String dueDate) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #090d16; color: #f8fafc; margin: 0; padding: 24px; }
                    .container { max-width: 580px; margin: 0 auto; background: #111827; border-radius: 16px; border: 1px solid rgba(255,255,255,0.12); padding: 32px; box-shadow: 0 10px 30px rgba(0,0,0,0.5); }
                    .badge { display: inline-block; padding: 6px 14px; background: rgba(16, 185, 129, 0.15); color: #34d399; border-radius: 20px; font-weight: 700; font-size: 12px; text-transform: uppercase; margin-bottom: 16px; border: 1px solid rgba(16, 185, 129, 0.4); letter-spacing: 0.5px; }
                    h2 { margin: 0 0 12px; color: #ffffff; font-size: 24px; }
                    .info-card { background: #1e293b; border-radius: 12px; padding: 20px; margin: 20px 0; border: 1px solid rgba(255,255,255,0.08); }
                    .info-row { display: flex; justify-content: space-between; padding: 10px 0; border-bottom: 1px solid rgba(255,255,255,0.06); font-size: 14px; }
                    .info-row:last-child { border-bottom: none; }
                    .info-label { color: #94a3b8; font-weight: 500; }
                    .info-value { color: #f1f5f9; font-weight: 600; }
                    .notice { color: #f59e0b; font-size: 14px; margin-top: 20px; padding: 14px; background: rgba(245, 158, 11, 0.12); border-left: 4px solid #f59e0b; border-radius: 6px; }
                    .footer { margin-top: 28px; font-size: 12px; color: #64748b; text-align: center; border-top: 1px solid rgba(255,255,255,0.08); padding-top: 20px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="badge">College Library &bull; Circulation Notice</div>
                    <h2>Book Issued Successfully</h2>
                    <p style="color: #cbd5e1; font-size: 15px; margin: 0 0 16px;">Hello <strong>%s</strong>, your requested library book has been issued.</p>
                    <div class="info-card">
                        <div class="info-row"><span class="info-label">Student:</span><span class="info-value">%s (%s)</span></div>
                        <div class="info-row"><span class="info-label">Department:</span><span class="info-value">%s (Section %s, Year %d)</span></div>
                        <div class="info-row"><span class="info-label">Book Title:</span><span class="info-value" style="color:#38bdf8;">%s</span></div>
                        <div class="info-row"><span class="info-label">Author:</span><span class="info-value">%s</span></div>
                        <div class="info-row"><span class="info-label">Issue Date:</span><span class="info-value">%s</span></div>
                        <div class="info-row"><span class="info-label">Return Due Date:</span><span class="info-value" style="color:#f43f5e; font-size:15px;">%s</span></div>
                    </div>
                    <div class="notice">
                        <strong>Notice:</strong> Please return the book before the return date to avoid late library fees.
                    </div>
                    <div class="footer">
                        Automated notification from College Library Management System &bull; Department of Information Technology
                    </div>
                </div>
            </body>
            </html>
            """,
                student.getName(),
                student.getName(), student.getStudentId(),
                student.getDepartment(), student.getSection(), student.getYear(),
                book.getTitle(),
                book.getAuthor(),
                issueDate,
                dueDate
        );
    }

    private String buildReturnedHtml(Student student, Book book, String returnDate) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #090d16; color: #f8fafc; margin: 0; padding: 24px; }
                    .container { max-width: 580px; margin: 0 auto; background: #111827; border-radius: 16px; border: 1px solid rgba(255,255,255,0.12); padding: 32px; box-shadow: 0 10px 30px rgba(0,0,0,0.5); }
                    .badge { display: inline-block; padding: 6px 14px; background: rgba(56, 189, 248, 0.15); color: #38bdf8; border-radius: 20px; font-weight: 700; font-size: 12px; text-transform: uppercase; margin-bottom: 16px; border: 1px solid rgba(56, 189, 248, 0.4); letter-spacing: 0.5px; }
                    h2 { margin: 0 0 12px; color: #ffffff; font-size: 24px; }
                    .info-card { background: #1e293b; border-radius: 12px; padding: 20px; margin: 20px 0; border: 1px solid rgba(255,255,255,0.08); }
                    .info-row { display: flex; justify-content: space-between; padding: 10px 0; border-bottom: 1px solid rgba(255,255,255,0.06); font-size: 14px; }
                    .info-row:last-child { border-bottom: none; }
                    .info-label { color: #94a3b8; font-weight: 500; }
                    .info-value { color: #f1f5f9; font-weight: 600; }
                    .notice-green { color: #34d399; font-size: 14px; margin-top: 20px; padding: 14px; background: rgba(16, 185, 129, 0.12); border-left: 4px solid #10b981; border-radius: 6px; }
                    .footer { margin-top: 28px; font-size: 12px; color: #64748b; text-align: center; border-top: 1px solid rgba(255,255,255,0.08); padding-top: 20px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="badge">College Library &bull; Official Return Receipt</div>
                    <h2>Book Returned Successfully</h2>
                    <p style="color: #cbd5e1; font-size: 15px; margin: 0 0 16px;">Hello <strong>%s</strong>, your returned book has been inspected and checked back into the library catalog.</p>
                    <div class="info-card">
                        <div class="info-row"><span class="info-label">Student:</span><span class="info-value">%s (%s)</span></div>
                        <div class="info-row"><span class="info-label">Book Title:</span><span class="info-value" style="color:#38bdf8;">%s</span></div>
                        <div class="info-row"><span class="info-label">Author:</span><span class="info-value">%s</span></div>
                        <div class="info-row"><span class="info-label">Returned Date:</span><span class="info-value">%s</span></div>
                        <div class="info-row"><span class="info-label">Return Status:</span><span class="info-value" style="color:#10b981;">Returned in Good Condition</span></div>
                    </div>
                    <div class="notice-green">
                        <strong>Receipt Verified:</strong> Thank you for returning the book on time! Your library borrowing record is clear.
                    </div>
                    <div class="footer">
                        Automated notification from College Library Management System &bull; Department of Information Technology
                    </div>
                </div>
            </body>
            </html>
            """,
                student.getName(),
                student.getName(), student.getStudentId(),
                book.getTitle(),
                book.getAuthor(),
                returnDate
        );
    }

    private String buildWaitlistJoinedHtml(Student student, Book book) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #090d16; color: #f8fafc; margin: 0; padding: 24px; }
                    .container { max-width: 580px; margin: 0 auto; background: #111827; border-radius: 16px; border: 1px solid rgba(255,255,255,0.12); padding: 32px; box-shadow: 0 10px 30px rgba(0,0,0,0.5); }
                    .badge { display: inline-block; padding: 6px 14px; background: rgba(245, 158, 11, 0.15); color: #fbbf24; border-radius: 20px; font-weight: 700; font-size: 12px; text-transform: uppercase; margin-bottom: 16px; border: 1px solid rgba(245, 158, 11, 0.4); letter-spacing: 0.5px; }
                    h2 { margin: 0 0 12px; color: #ffffff; font-size: 24px; }
                    .info-card { background: #1e293b; border-radius: 12px; padding: 20px; margin: 20px 0; border: 1px solid rgba(255,255,255,0.08); }
                    .info-row { display: flex; justify-content: space-between; padding: 10px 0; border-bottom: 1px solid rgba(255,255,255,0.06); font-size: 14px; }
                    .info-row:last-child { border-bottom: none; }
                    .info-label { color: #94a3b8; font-weight: 500; }
                    .info-value { color: #f1f5f9; font-weight: 600; }
                    .notice-amber { color: #f59e0b; font-size: 14px; margin-top: 20px; padding: 14px; background: rgba(245, 158, 11, 0.12); border-left: 4px solid #f59e0b; border-radius: 6px; }
                    .footer { margin-top: 28px; font-size: 12px; color: #64748b; text-align: center; border-top: 1px solid rgba(255,255,255,0.08); padding-top: 20px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="badge">College Library &bull; Priority Waitlist</div>
                    <h2>Waitlist Confirmation</h2>
                    <p style="color: #cbd5e1; font-size: 15px; margin: 0 0 16px;">Hello <strong>%s</strong>, you have been added to the priority waitlist for this out-of-stock book.</p>
                    <div class="info-card">
                        <div class="info-row"><span class="info-label">Student:</span><span class="info-value">%s (%s)</span></div>
                        <div class="info-row"><span class="info-label">Requested Book:</span><span class="info-value" style="color:#38bdf8;">%s</span></div>
                        <div class="info-row"><span class="info-label">Author:</span><span class="info-value">%s</span></div>
                        <div class="info-row"><span class="info-label">Queue Status:</span><span class="info-value" style="color:#fbbf24;">Active Waitlist Priority #1</span></div>
                    </div>
                    <div class="notice-amber">
                        <strong>Automatic Alert Activated:</strong> As soon as any student returns a copy of this book, our system will automatically dispatch an email alert to you so you can borrow it immediately.
                    </div>
                    <div class="footer">
                        Automated notification from College Library Management System &bull; Department of Information Technology
                    </div>
                </div>
            </body>
            </html>
            """,
                student.getName(),
                student.getName(), student.getStudentId(),
                book.getTitle(),
                book.getAuthor()
        );
    }

    private String buildBookAvailableHtml(Student student, Book book) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #090d16; color: #f8fafc; margin: 0; padding: 24px; }
                    .container { max-width: 580px; margin: 0 auto; background: #111827; border-radius: 16px; border: 1px solid rgba(255,255,255,0.12); padding: 32px; box-shadow: 0 10px 30px rgba(0,0,0,0.5); }
                    .badge { display: inline-block; padding: 6px 14px; background: rgba(16, 185, 129, 0.15); color: #34d399; border-radius: 20px; font-weight: 700; font-size: 12px; text-transform: uppercase; margin-bottom: 16px; border: 1px solid rgba(16, 185, 129, 0.4); letter-spacing: 0.5px; }
                    h2 { margin: 0 0 12px; color: #ffffff; font-size: 24px; }
                    .info-card { background: #1e293b; border-radius: 12px; padding: 20px; margin: 20px 0; border: 1px solid rgba(255,255,255,0.08); }
                    .info-row { display: flex; justify-content: space-between; padding: 10px 0; border-bottom: 1px solid rgba(255,255,255,0.06); font-size: 14px; }
                    .info-row:last-child { border-bottom: none; }
                    .info-label { color: #94a3b8; font-weight: 500; }
                    .info-value { color: #f1f5f9; font-weight: 600; }
                    .notice-cyan { color: #38bdf8; font-size: 14px; margin-top: 20px; padding: 14px; background: rgba(6, 182, 212, 0.12); border-left: 4px solid #06b6d4; border-radius: 6px; }
                    .footer { margin-top: 28px; font-size: 12px; color: #64748b; text-align: center; border-top: 1px solid rgba(255,255,255,0.08); padding-top: 20px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="badge">Book Ready For Pickup!</div>
                    <h2>Your Reserved Book Is Ready to Collect</h2>
                    <p style="color: #cbd5e1; font-size: 15px; margin: 0 0 16px;">Hello <strong>%s</strong>, your reserved book is now available in the library.</p>
                    <div class="info-card">
                        <div class="info-row"><span class="info-label">Student:</span><span class="info-value">%s (%s)</span></div>
                        <div class="info-row"><span class="info-label">Book Title:</span><span class="info-value" style="color:#38bdf8;">%s</span></div>
                        <div class="info-row"><span class="info-label">Author:</span><span class="info-value">%s</span></div>
                        <div class="info-row"><span class="info-label">Pickup Location:</span><span class="info-value">Central Library Circulation Desk</span></div>
                    </div>
                    <div class="notice-cyan">
                        <strong>Ready for Collection:</strong> The book is now available at the library circulation desk. Please visit the college library and collect your book.
                    </div>
                    <div class="footer">
                        Notification from College Library Management System &bull; Department of Information Technology
                    </div>
                </div>
            </body>
            </html>
            """,
                student.getName(),
                student.getName(), student.getStudentId(),
                book.getTitle(),
                book.getAuthor()
        );
    }

    public boolean isSmtpConfigured() {
        return smtpUsername != null && !smtpUsername.isBlank();
    }

    /**
     * Sends an official due date reminder email to student whose book is due today.
     */
    public boolean sendDueReminderEmail(String studentEmail, String studentName, String studentId, String bookTitle, String dueDate, String fineRate) {
        if (studentEmail == null || studentEmail.isBlank()) {
            log.warn("Cannot send due reminder: Student email is missing.");
            return false;
        }

        String recipientEmail = studentEmail.trim();
        String subject = "⏰ Return Reminder: '" + bookTitle + "' is Due Today - College Library";

        String textContent = String.format(
                "College Library Book Return Reminder\n\n" +
                "Student: %s (%s)\n" +
                "Book: %s\n" +
                "Due Cutoff: %s (Today by 17:00 IST)\n" +
                "Same-Day Fee: ₹0.00 (Grace period active until 5:00 PM)\n" +
                "Late Fine Rate: %s starting tomorrow morning\n\n" +
                "Please return the book to the library circulation desk before 17:00 IST today.",
                studentName, studentId,
                bookTitle,
                dueDate,
                fineRate
        );

        String htmlContent = buildDueReminderHtml(studentName, studentId, bookTitle, dueDate, fineRate);

        return dispatchEmail(recipientEmail, studentName, subject, "DUE_TODAY_REMINDER", textContent, htmlContent);
    }

    private String buildDueReminderHtml(String studentName, String studentId, String bookTitle, String dueDate, String fineRate) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #090d16; color: #f8fafc; margin: 0; padding: 24px; }
                    .container { max-width: 580px; margin: 0 auto; background: #111827; border-radius: 16px; border: 1px solid rgba(255,255,255,0.12); padding: 32px; box-shadow: 0 10px 30px rgba(0,0,0,0.5); }
                    .badge { display: inline-block; padding: 6px 14px; background: rgba(56, 189, 248, 0.15); color: #38bdf8; border-radius: 20px; font-weight: 700; font-size: 12px; text-transform: uppercase; margin-bottom: 16px; border: 1px solid rgba(56, 189, 248, 0.4); letter-spacing: 0.5px; }
                    h2 { margin: 0 0 12px; color: #ffffff; font-size: 24px; }
                    .info-card { background: #1e293b; border-radius: 12px; padding: 20px; margin: 20px 0; border: 1px solid rgba(255,255,255,0.08); }
                    .info-row { display: flex; justify-content: space-between; padding: 10px 0; border-bottom: 1px solid rgba(255,255,255,0.06); font-size: 14px; }
                    .info-row:last-child { border-bottom: none; }
                    .info-label { color: #94a3b8; font-weight: 500; }
                    .info-value { color: #f1f5f9; font-weight: 600; }
                    .notice-amber { color: #fbbf24; font-size: 14px; margin-top: 20px; padding: 14px; background: rgba(245, 158, 11, 0.12); border-left: 4px solid #f59e0b; border-radius: 6px; }
                    .footer { margin-top: 28px; font-size: 12px; color: #64748b; text-align: center; border-top: 1px solid rgba(255,255,255,0.08); padding-top: 20px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="badge">⏰ Official Return Reminder</div>
                    <h2>Book Due Today - College Library</h2>
                    <p style="color: #cbd5e1; font-size: 15px; margin: 0 0 16px;">Dear <strong>%s</strong>, this is an official reminder that your borrowed book is scheduled for return today.</p>
                    <div class="info-card">
                        <div class="info-row"><span class="info-label">Student Name:</span><span class="info-value">%s</span></div>
                        <div class="info-row"><span class="info-label">Student ID:</span><span class="info-value" style="color:#38bdf8;">%s</span></div>
                        <div class="info-row"><span class="info-label">Book Title:</span><span class="info-value" style="color:#ffffff;">%s</span></div>
                        <div class="info-row"><span class="info-label">Due Cutoff:</span><span class="info-value" style="color:#fbbf24; font-weight:700;">%s (Today by 17:00 IST)</span></div>
                        <div class="info-row"><span class="info-label">Same-Day Fee:</span><span class="info-value" style="color:#34d399; font-weight:700;">₹0.00 (Grace Active)</span></div>
                        <div class="info-row"><span class="info-label">Late Fine Trigger:</span><span class="info-value" style="color:#fb7185; font-weight:700;">%s starting tomorrow morning</span></div>
                    </div>
                    <div class="notice-amber">
                        <strong>Action Required:</strong> Please return this textbook to the Library Circulation Desk before 5:00 PM today to avoid automated late fee accrual.
                    </div>
                    <div class="footer">
                        Easwari Engineering College &bull; Department of Information Technology Library Portal
                    </div>
                </div>
            </body>
            </html>
            """,
                studentName,
                studentName,
                studentId,
                bookTitle,
                dueDate,
                fineRate
        );
    }
}
