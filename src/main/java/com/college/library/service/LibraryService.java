package com.college.library.service;

import com.college.library.dto.BookDto;
import com.college.library.dto.IssueRequest;
import com.college.library.dto.TransactionResponse;
import com.college.library.dto.WaitlistResponse;
import com.college.library.entity.Book;
import com.college.library.entity.BorrowTransaction;
import com.college.library.entity.Student;
import com.college.library.entity.TransactionStatus;
import com.college.library.entity.WaitlistReservation;
import com.college.library.entity.WaitlistStatus;
import com.college.library.repository.BookRepository;
import com.college.library.repository.BorrowTransactionRepository;
import com.college.library.repository.StudentRepository;
import com.college.library.repository.WaitlistReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LibraryService {

    private static final Logger log = LoggerFactory.getLogger(LibraryService.class);

    private final StudentRepository studentRepository;
    private final BookRepository bookRepository;
    private final BorrowTransactionRepository transactionRepository;
    private final WaitlistReservationRepository waitlistRepository;
    private final EmailService emailService;

    @Value("${library.loan.duration-days:14}")
    private int loanDurationDays;

    public LibraryService(StudentRepository studentRepository,
                          BookRepository bookRepository,
                          BorrowTransactionRepository transactionRepository,
                          WaitlistReservationRepository waitlistRepository,
                          EmailService emailService) {
        this.studentRepository = studentRepository;
        this.bookRepository = bookRepository;
        this.transactionRepository = transactionRepository;
        this.waitlistRepository = waitlistRepository;
        this.emailService = emailService;
    }

    public Student getStudentByIdOrBarcode(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Student ID, Barcode, or Email cannot be empty");
        }
        String cleanQuery = query.trim();
        return studentRepository.findByStudentIdIgnoreCase(cleanQuery)
                .or(() -> studentRepository.findByBarcode(cleanQuery))
                .or(() -> studentRepository.findByEmailIgnoreCase(cleanQuery))
                .orElseThrow(() -> new IllegalArgumentException("Student not found with ID/Barcode/Email: " + cleanQuery));
    }

    @Transactional
    public Student saveStudent(Student student) {
        if (student.getStudentId() == null || student.getStudentId().isBlank()) {
            throw new IllegalArgumentException("Student ID is required");
        }
        if (student.getEmail() == null || student.getEmail().isBlank()) {
            throw new IllegalArgumentException("Student email is required");
        }
        if (student.getBarcode() == null || student.getBarcode().isBlank()) {
            student.setBarcode(student.getStudentId());
        }
        return studentRepository.findByStudentIdIgnoreCase(student.getStudentId())
                .map(existing -> {
                    existing.setName(student.getName());
                    existing.setEmail(student.getEmail());
                    existing.setDepartment(student.getDepartment());
                    existing.setSection(student.getSection());
                    existing.setYear(student.getYear());
                    existing.setBarcode(student.getBarcode());
                    return studentRepository.save(existing);
                })
                .orElseGet(() -> studentRepository.save(student));
    }

    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    public List<Book> getAllBooks() {
        return bookRepository.findAllByOrderByTitleAsc();
    }

    public List<Book> searchBooks(String query) {
        if (query == null || query.isBlank()) {
            return getAllBooks();
        }
        return bookRepository.searchBooks(query.trim());
    }

    public Book getBookById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Book not found with ID: " + id));
    }

    @Transactional
    public Book addBook(BookDto bookDto) {
        if (bookRepository.existsByIsbn(bookDto.getIsbn())) {
            throw new IllegalArgumentException("A book with ISBN " + bookDto.getIsbn() + " already exists.");
        }
        int copies = bookDto.getTotalCopies() != null ? bookDto.getTotalCopies() : 1;
        Book book = new Book(
                bookDto.getIsbn(),
                bookDto.getTitle(),
                bookDto.getAuthor(),
                bookDto.getCategory(),
                copies,
                copies
        );
        return bookRepository.save(book);
    }

    /**
     * Issues a book to a student.
     * Decrements available copies by 1, calculates 14-day due date, logs transaction,
     * and sends a confirmation email to the student.
     */
    @Transactional
    public TransactionResponse issueBook(IssueRequest request) {
        Student student = getStudentByIdOrBarcode(request.getStudentId());
        Book book = getBookById(request.getBookId());

        if (book.getAvailableCopies() <= 0) {
            throw new IllegalStateException("No available copies of '" + book.getTitle() + "' to borrow. You can join the waiting list.");
        }

        // Decrement available copies
        book.setAvailableCopies(book.getAvailableCopies() - 1);
        bookRepository.save(book);

        LocalDate issueDate = LocalDate.now();
        LocalDate dueDate = issueDate.plusDays(loanDurationDays);

        BorrowTransaction tx = new BorrowTransaction(student, book, issueDate, dueDate);
        tx = transactionRepository.save(tx);

        // Send email notification to student
        boolean emailSent = emailService.sendBookIssuedEmail(student, book, issueDate, dueDate);
        tx.setEmailSent(emailSent);
        tx = transactionRepository.save(tx);

        return TransactionResponse.fromEntity(tx);
    }

    /**
     * Returns an issued book.
     * Increases available copies by 1, sets return date, marks status as RETURNED,
     * dispatches return receipt email, and if there are students waiting for this book,
     * automatically notifies the next student via email that the book is available!
     */
    @Transactional
    public TransactionResponse returnBook(Long transactionId) {
        BorrowTransaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found with ID: " + transactionId));

        if (tx.getStatus() == TransactionStatus.RETURNED) {
            throw new IllegalStateException("This book has already been returned.");
        }

        Book book = tx.getBook();
        book.setAvailableCopies(Math.min(book.getTotalCopies(), book.getAvailableCopies() + 1));
        bookRepository.save(book);

        tx.setReturnDate(LocalDate.now());
        tx.setStatus(TransactionStatus.RETURNED);
        tx = transactionRepository.save(tx);

        // Send official return receipt email to returning student
        boolean returnEmailSent = emailService.sendBookReturnedEmail(tx.getStudent(), book, tx.getReturnDate());
        tx.setEmailSent(returnEmailSent);
        tx = transactionRepository.save(tx);

        // Check if any student is on the waiting list for this newly available book
        List<WaitlistReservation> waitingQueue = waitlistRepository
                .findByBookIdAndStatusOrderByReservationDateAsc(book.getId(), WaitlistStatus.WAITING);

        TransactionResponse response = TransactionResponse.fromEntity(tx);
        if (!waitingQueue.isEmpty()) {
            response.setWaitingStudents(waitingQueue.stream()
                    .map(WaitlistResponse::fromEntity)
                    .collect(Collectors.toList()));
            log.info("Book '{}' returned. {} student(s) waiting in Reservations.", book.getTitle(), waitingQueue.size());
        }

        return response;
    }

    /**
     * Manually sends an availability notification email to a student waiting in Reservations
     * and updates the reservation status to NOTIFIED.
     */
    @Transactional
    public WaitlistResponse notifyStudent(Long waitlistId) {
        WaitlistReservation reservation = waitlistRepository.findById(waitlistId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found with ID: " + waitlistId));

        boolean sent = emailService.sendBookAvailableEmail(reservation.getStudent(), reservation.getBook());
        reservation.setStatus(WaitlistStatus.NOTIFIED);
        reservation.setNotificationDate(LocalDate.now());
        reservation.setEmailSent(sent);
        reservation = waitlistRepository.save(reservation);

        log.info("Librarian manually notified student {} ({}) that '{}' is available.",
                reservation.getStudent().getName(), reservation.getStudent().getEmail(), reservation.getBook().getTitle());

        return WaitlistResponse.fromEntity(reservation);
    }

    /**
     * Adds a student to the waitlist when a book is out of stock (availableCopies == 0).
     * Automatically dispatches a priority waitlist confirmation email.
     */
    @Transactional
    public WaitlistResponse joinWaitlist(String studentId, Long bookId) {
        Student student = getStudentByIdOrBarcode(studentId);
        Book book = getBookById(bookId);

        if (book.getAvailableCopies() > 0) {
            throw new IllegalStateException("'" + book.getTitle() + "' currently has " + book.getAvailableCopies() + " available copies! You can borrow it directly.");
        }

        // Check if student is already waiting for this book
        var existing = waitlistRepository.findByBookIdAndStudentIdAndStatus(book.getId(), student.getId(), WaitlistStatus.WAITING);
        if (existing.isPresent()) {
            return WaitlistResponse.fromEntity(existing.get());
        }

        WaitlistReservation reservation = new WaitlistReservation(student, book);
        reservation = waitlistRepository.save(reservation);

        // Dispatch confirmation email to student
        boolean emailSent = emailService.sendWaitlistConfirmationEmail(student, book);
        reservation.setEmailSent(emailSent);
        reservation = waitlistRepository.save(reservation);

        log.info("Student {} ({}) joined waitlist for book '{}'", student.getName(), student.getEmail(), book.getTitle());
        return WaitlistResponse.fromEntity(reservation);
    }

    public List<WaitlistResponse> getActiveWaitlist() {
        return waitlistRepository.findByStatusOrderByReservationDateDesc(WaitlistStatus.WAITING)
                .stream()
                .map(WaitlistResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<WaitlistResponse> getAllWaitlist() {
        return waitlistRepository.findAllByOrderByReservationDateDesc()
                .stream()
                .map(WaitlistResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<TransactionResponse> getActiveTransactions() {
        return transactionRepository.findByStatusOrderByIssueDateDesc(TransactionStatus.ISSUED)
                .stream()
                .map(TransactionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<TransactionResponse> getAllTransactions() {
        return transactionRepository.findAllByOrderByIdDesc()
                .stream()
                .map(TransactionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<TransactionResponse> getStudentTransactions(String studentId) {
        Student student = getStudentByIdOrBarcode(studentId);
        return transactionRepository.findByStudentOrderByIssueDateDesc(student)
                .stream()
                .map(TransactionResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
