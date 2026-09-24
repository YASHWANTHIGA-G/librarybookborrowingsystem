package com.college.library.config;

import com.college.library.entity.Book;
import com.college.library.entity.Student;
import com.college.library.entity.WaitlistReservation;
import com.college.library.entity.WaitlistStatus;
import com.college.library.repository.BookRepository;
import com.college.library.repository.StudentRepository;
import com.college.library.repository.WaitlistReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.util.List;

@Configuration
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final StudentRepository studentRepository;
    private final BookRepository bookRepository;
    private final WaitlistReservationRepository waitlistReservationRepository;

    public DataInitializer(StudentRepository studentRepository, BookRepository bookRepository, WaitlistReservationRepository waitlistReservationRepository) {
        this.studentRepository = studentRepository;
        this.bookRepository = bookRepository;
        this.waitlistReservationRepository = waitlistReservationRepository;
    }

    @Override
    public void run(String... args) {
        log.info("Initializing Library Database with Yashwanthiga student demo profile and book catalog...");

        // Ensure Yashwanthiga exists in the database
        studentRepository.findByStudentIdIgnoreCase("EECIT001").ifPresentOrElse(
                student -> log.info("Demo student Yashwanthiga (EECIT001) already present in database."),
                () -> {
                    Student yashwanthiga = new Student(
                            "EECIT001",
                            "Yashwanthiga",
                            "IT",
                            "E",
                            2,
                            "yashwanthiga@gmail.com",
                            "EECIT001"
                    );
                    studentRepository.save(yashwanthiga);
                    log.info("Demo student Yashwanthiga (EECIT001) successfully registered in database.");
                }
        );

        // Ensure Priya exists in the database
        studentRepository.findByStudentIdIgnoreCase("310625205283").ifPresentOrElse(
                student -> log.info("Demo student Priya (310625205283) already present in database."),
                () -> {
                    Student priya = new Student(
                            "310625205283",
                            "Priya",
                            "IT",
                            "E",
                            2,
                            "310625205283@eec.srmrmp.edu.in",
                            "310625205283"
                    );
                    studentRepository.save(priya);
                    log.info("Demo student Priya (310625205283) successfully registered in database.");
                }
        );

        // Ensure Tanisha Balaji exists in the database
        studentRepository.findByEmailIgnoreCase("tanishabalaji7@gmail.com")
                .or(() -> studentRepository.findByStudentIdIgnoreCase("EECIT002"))
                .ifPresentOrElse(
                        student -> log.info("Demo student Tanisha Balaji ({}) already present in database.", student.getStudentId()),
                        () -> {
                            Student tanisha = new Student(
                                    "EECIT002",
                                    "Tanisha Balaji",
                                    "IT",
                                    "E",
                                    2,
                                    "tanishabalaji7@gmail.com",
                                    "EECIT002"
                            );
                            studentRepository.save(tanisha);
                            log.info("Demo student Tanisha Balaji (EECIT002 / tanishabalaji7@gmail.com) successfully registered in database.");
                        }
                );

        // Ensure Karthik exists in the database
        studentRepository.findByStudentIdIgnoreCase("EECIT003").ifPresentOrElse(
                student -> log.info("Demo student Karthik (EECIT003) already present in database."),
                () -> {
                    Student karthik = new Student(
                            "EECIT003",
                            "Karthik",
                            "IT",
                            "E",
                            2,
                            "karthik@eec.srmrmp.edu.in",
                            "EECIT003"
                    );
                    studentRepository.save(karthik);
                    log.info("Demo student Karthik (EECIT003) successfully registered in database.");
                }
        );

        // Ensure Sindhureny exists in the database
        studentRepository.findByStudentIdIgnoreCase("EECIT004").ifPresentOrElse(
                student -> log.info("Demo student Sindhureny (EECIT004) already present in database."),
                () -> {
                    Student sindhureny = new Student(
                            "EECIT004",
                            "Sindhureny",
                            "IT",
                            "E",
                            2,
                            "sindhureny@eec.srmrmp.edu.in",
                            "EECIT004"
                    );
                    studentRepository.save(sindhureny);
                    log.info("Demo student Sindhureny (EECIT004) successfully registered in database.");
                }
        );

        // Ensure Vishnu exists in the database
        studentRepository.findByStudentIdIgnoreCase("EECIT005").ifPresentOrElse(
                student -> log.info("Demo student Vishnu (EECIT005) already present in database."),
                () -> {
                    Student vishnu = new Student(
                            "EECIT005",
                            "Vishnu",
                            "IT",
                            "E",
                            2,
                            "vishnu@eec.srmrmp.edu.in",
                            "EECIT005"
                    );
                    studentRepository.save(vishnu);
                    log.info("Demo student Vishnu (EECIT005) successfully registered in database.");
                }
        );

        // Ensure Tarunshree exists in the database
        studentRepository.findByStudentIdIgnoreCase("EECIT006").ifPresentOrElse(
                student -> log.info("Demo student Tarunshree (EECIT006) already present in database."),
                () -> {
                    Student tarunshree = new Student(
                            "EECIT006",
                            "Tarunshree",
                            "IT",
                            "E",
                            2,
                            "tarunshree@eec.srmrmp.edu.in",
                            "EECIT006"
                    );
                    studentRepository.save(tarunshree);
                    log.info("Demo student Tarunshree (EECIT006) successfully registered in database.");
                }
        );

        // Seed initial library books if catalog is empty
        if (bookRepository.count() == 0) {
            List<Book> initialBooks = List.of(
                    new Book("978-1-032-78600-1", "Fundamentals of Data Science", "Sanjeev J. Wagh, Manisha S. Bhende, Anuradha D. Thakare", "Data Science", 5, 5),
                    new Book("978-0132350884", "Clean Code", "Robert C. Martin", "Software Engineering", 4, 4),
                    new Book("978-0134685991", "Effective Java", "Joshua Bloch", "Java Programming", 3, 3),
                    new Book("978-0201633610", "Design Patterns: Elements of Reusable Object-Oriented Software", "Erich Gamma, Richard Helm, Ralph Johnson, John Vlissides", "Software Architecture", 2, 2),
                    new Book("978-0262033848", "Introduction to Algorithms", "Thomas H. Cormen", "Computer Science", 5, 5),
                    new Book("978-0133594140", "Computer Networking: A Top-Down Approach", "James F. Kurose, Keith W. Ross", "Networking", 3, 3),
                    new Book("978-0078022159", "Database System Concepts", "Abraham Silberschatz, Henry F. Korth", "Databases", 4, 4)
            );
            bookRepository.saveAll(initialBooks);
            log.info("Seeded {} initial curriculum books into catalog.", initialBooks.size());
        } else {
            List<Book> foundBooks = bookRepository.findByTitleContainingIgnoreCase("Fundamentals of Data Science");
            if (!foundBooks.isEmpty()) {
                Book book = foundBooks.get(0);
                book.setIsbn("978-1-032-78600-1");
                bookRepository.save(book);
                log.info("Updated Fundamentals of Data Science ISBN to 978-1-032-78600-1");
            } else if (!bookRepository.existsByIsbn("978-1-032-78600-1")) {
                bookRepository.save(new Book("978-1-032-78600-1", "Fundamentals of Data Science", "Sanjeev J. Wagh, Manisha S. Bhende, Anuradha D. Thakare", "Data Science", 5, 5));
                log.info("Registered Fundamentals of Data Science in library catalog.");
            }
        }

        // Seed initial waiting list reservations for demonstration
        if (waitlistReservationRepository.count() == 0) {
            log.info("Seeding initial waiting list reservations for demo...");
            var priyaOpt = studentRepository.findByStudentIdIgnoreCase("310625205283");
            var yashwanthigaOpt = studentRepository.findByStudentIdIgnoreCase("EECIT001");
            var tanishaOpt = studentRepository.findByStudentIdIgnoreCase("EECIT002");
            var karthikOpt = studentRepository.findByStudentIdIgnoreCase("EECIT003");

            List<Book> allBooks = bookRepository.findAll();
            if (!allBooks.isEmpty()) {
                Book cleanCode = allBooks.stream().filter(b -> b.getTitle().toLowerCase().contains("clean code")).findFirst().orElse(allBooks.get(0));
                Book dataScience = allBooks.stream().filter(b -> b.getTitle().toLowerCase().contains("data science")).findFirst().orElse(allBooks.get(0));
                Book effectiveJava = allBooks.stream().filter(b -> b.getTitle().toLowerCase().contains("effective java")).findFirst().orElse(allBooks.get(0));
                Book algo = allBooks.stream().filter(b -> b.getTitle().toLowerCase().contains("algorithms")).findFirst().orElse(allBooks.get(0));

                if (priyaOpt.isPresent()) {
                    WaitlistReservation res1 = new WaitlistReservation(priyaOpt.get(), cleanCode);
                    res1.setReservationDate(LocalDate.now().minusDays(2));
                    res1.setStatus(WaitlistStatus.WAITING);
                    waitlistReservationRepository.save(res1);
                }

                if (yashwanthigaOpt.isPresent()) {
                    WaitlistReservation res2 = new WaitlistReservation(yashwanthigaOpt.get(), dataScience);
                    res2.setReservationDate(LocalDate.now().minusDays(1));
                    res2.setStatus(WaitlistStatus.WAITING);
                    waitlistReservationRepository.save(res2);
                }

                if (tanishaOpt.isPresent()) {
                    WaitlistReservation res3 = new WaitlistReservation(tanishaOpt.get(), effectiveJava);
                    res3.setReservationDate(LocalDate.now());
                    res3.setStatus(WaitlistStatus.WAITING);
                    waitlistReservationRepository.save(res3);
                }

                if (karthikOpt.isPresent()) {
                    WaitlistReservation res4 = new WaitlistReservation(karthikOpt.get(), algo);
                    res4.setReservationDate(LocalDate.now());
                    res4.setStatus(WaitlistStatus.WAITING);
                    waitlistReservationRepository.save(res4);
                }
                log.info("Seeded initial waiting list reservations successfully.");
            }
        }
    }
}
