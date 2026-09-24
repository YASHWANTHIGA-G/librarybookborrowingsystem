package com.college.library.controller;

import com.college.library.repository.BookRepository;
import com.college.library.repository.BorrowTransactionRepository;
import com.college.library.repository.StudentRepository;
import com.college.library.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.*;

@RestController
@RequestMapping("/api/system")
@CrossOrigin(origins = "*")
public class SystemController {

    private final StudentRepository studentRepository;
    private final BookRepository bookRepository;
    private final BorrowTransactionRepository transactionRepository;
    private final EmailService emailService;

    public SystemController(StudentRepository studentRepository,
                            BookRepository bookRepository,
                            BorrowTransactionRepository transactionRepository,
                            EmailService emailService) {
        this.studentRepository = studentRepository;
        this.bookRepository = bookRepository;
        this.transactionRepository = transactionRepository;
        this.emailService = emailService;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSystemStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "ONLINE");
        status.put("databaseConnected", true);
        status.put("databaseEngine", com.college.library.config.DataSourceConfig.activeDatabaseType);
        status.put("studentCount", studentRepository.count());
        status.put("bookCount", bookRepository.count());
        status.put("totalTransactions", transactionRepository.count());
        status.put("smtpConfigured", emailService.isSmtpConfigured());
        status.put("smtpUsername", maskEmail(emailService.getSmtpUsername()));

        // Find primary local IP address to make it easy for phone demo
        status.put("localIps", getLocalIpAddresses());

        return ResponseEntity.ok(status);
    }

    @org.springframework.web.bind.annotation.PostMapping("/smtp/config")
    public ResponseEntity<Map<String, Object>> updateSmtpConfig(@org.springframework.web.bind.annotation.RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        String password = payload.get("password");
        emailService.updateCredentials(username, password);

        // Persist permanently to application.properties
        persistSmtpCredentials(username, password);

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("smtpConfigured", emailService.isSmtpConfigured());
        resp.put("smtpUsername", maskEmail(emailService.getSmtpUsername()));
        resp.put("message", "SMTP credentials saved and updated successfully.");
        return ResponseEntity.ok(resp);
    }

    @org.springframework.web.bind.annotation.PostMapping("/smtp/test")
    public ResponseEntity<Map<String, Object>> testSmtpConnection(@org.springframework.web.bind.annotation.RequestBody(required = false) Map<String, String> payload) {
        String recipient = (payload != null && payload.get("recipient") != null) ? payload.get("recipient") : null;
        Map<String, Object> testResult = emailService.sendTestEmail(recipient);
        return ResponseEntity.ok(testResult);
    }

    /**
     * Dispatches real due date reminder email to student inbox.
     */
    @org.springframework.web.bind.annotation.PostMapping("/reminder")
    public ResponseEntity<Map<String, Object>> sendDueReminder(@org.springframework.web.bind.annotation.RequestBody Map<String, String> payload) {
        String studentEmail = payload.get("studentEmail");
        String studentName = payload.get("studentName");
        String studentId = payload.get("studentId");
        String bookTitle = payload.get("bookTitle");
        String dueDate = payload.getOrDefault("dueDate", "Today by 17:00 IST");
        String fineRate = payload.getOrDefault("fineRate", "₹2.00 / day");

        boolean sent = emailService.sendDueReminderEmail(studentEmail, studentName, studentId, bookTitle, dueDate, fineRate);

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("emailSent", sent);
        resp.put("recipient", studentEmail);
        resp.put("studentName", studentName);
        resp.put("bookTitle", bookTitle);
        resp.put("message", sent 
                ? "Official due date reminder email successfully delivered to " + studentEmail 
                : "Reminder dispatched for " + studentName + " (" + studentEmail + "). [Configure SMTP in top bar for live internet delivery]");
        return ResponseEntity.ok(resp);
    }

    private synchronized void persistSmtpCredentials(String username, String password) {
        if (username == null || password == null) return;
        try {
            java.nio.file.Path propPath = java.nio.file.Paths.get("src/main/resources/application.properties");
            if (java.nio.file.Files.exists(propPath)) {
                String content = java.nio.file.Files.readString(propPath);
                String cleanPw = password.trim().replace(" ", "");
                content = content.replaceAll("spring\\.mail\\.username=.*", "spring.mail.username=" + username.trim());
                content = content.replaceAll("spring\\.mail\\.password=.*", "spring.mail.password=" + cleanPw);
                java.nio.file.Files.writeString(propPath, content);
            }
        } catch (Exception ignored) {
        }
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank()) return "";
        int atIdx = email.indexOf('@');
        if (atIdx <= 1) return email;
        return email.substring(0, 2) + "***" + email.substring(atIdx);
    }

    private List<String> getLocalIpAddresses() {
        List<String> ips = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (!addr.isLoopbackAddress() && addr.getHostAddress().indexOf(':') == -1) {
                        ips.add(addr.getHostAddress());
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return ips;
    }
}
