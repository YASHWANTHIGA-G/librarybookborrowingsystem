package com.college.library.controller;

import com.college.library.repository.BookRepository;
import com.college.library.repository.BorrowTransactionRepository;
import com.college.library.repository.StudentRepository;
import com.college.library.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

        Map<String, Object> mailStatus = emailService.getMailConfigStatus();
        status.put("smtpConfigured", mailStatus.get("isConfigured"));
        status.put("mailConfig", mailStatus);
        status.put("activeMailMode", mailStatus.get("activeMode"));
        status.put("smtpUsername", mailStatus.get("smtpUsername"));

        // Find primary local IP address to make it easy for phone demo
        status.put("localIps", getLocalIpAddresses());

        return ResponseEntity.ok(status);
    }

    @PostMapping("/mail/config")
    public ResponseEntity<Map<String, Object>> updateMailConfig(@RequestBody Map<String, String> payload) {
        String relayUrl = payload.get("relayUrl");
        String brevoKey = payload.get("brevoKey");
        String resendKey = payload.get("resendKey");
        String username = payload.get("username");
        String password = payload.get("password");

        emailService.updateMailConfig(relayUrl, brevoKey, resendKey, username, password);
        persistMailCredentials(relayUrl, brevoKey, resendKey, username, password);

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("mailConfig", emailService.getMailConfigStatus());
        resp.put("message", "Email configuration updated successfully.");
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/smtp/config")
    public ResponseEntity<Map<String, Object>> updateSmtpConfig(@RequestBody Map<String, String> payload) {
        return updateMailConfig(payload);
    }

    @PostMapping("/mail/test")
    public ResponseEntity<Map<String, Object>> testMailConnection(@RequestBody(required = false) Map<String, String> payload) {
        String recipient = (payload != null && payload.get("recipient") != null) ? payload.get("recipient") : null;
        Map<String, Object> testResult = emailService.sendTestEmail(recipient);
        return ResponseEntity.ok(testResult);
    }

    @PostMapping("/smtp/test")
    public ResponseEntity<Map<String, Object>> testSmtpConnection(@RequestBody(required = false) Map<String, String> payload) {
        return testMailConnection(payload);
    }

    /**
     * Dispatches real due date reminder email to student inbox.
     */
    @PostMapping("/reminder")
    public ResponseEntity<Map<String, Object>> sendDueReminder(@RequestBody Map<String, String> payload) {
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
                : "Reminder dispatched for " + studentName + " (" + studentEmail + "). [Configure Email Relay/SMTP in Settings for live internet delivery]");
        return ResponseEntity.ok(resp);
    }

    private synchronized void persistMailCredentials(String relayUrl, String brevoKey, String resendKey, String username, String password) {
        try {
            java.nio.file.Path propPath = java.nio.file.Paths.get("src/main/resources/application.properties");
            if (java.nio.file.Files.exists(propPath)) {
                String content = java.nio.file.Files.readString(propPath);
                if (relayUrl != null) {
                    content = upsertProperty(content, "mail.relay.url", relayUrl.trim());
                }
                if (brevoKey != null) {
                    content = upsertProperty(content, "mail.brevo.api-key", brevoKey.trim());
                }
                if (resendKey != null) {
                    content = upsertProperty(content, "mail.resend.api-key", resendKey.trim());
                }
                if (username != null) {
                    content = upsertProperty(content, "spring.mail.username", username.trim());
                }
                if (password != null) {
                    content = upsertProperty(content, "spring.mail.password", password.trim().replace(" ", ""));
                }
                java.nio.file.Files.writeString(propPath, content);
            }
        } catch (Exception ignored) {
        }
    }

    private String upsertProperty(String content, String key, String val) {
        String escapedKey = key.replace(".", "\\.");
        if (content.matches("(?s).*^" + escapedKey + "=.*")) {
            return content.replaceAll("(?m)^" + escapedKey + "=.*", key + "=" + val);
        } else {
            return content + "\n" + key + "=" + val;
        }
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
