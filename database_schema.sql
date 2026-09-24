-- ============================================================================
-- COLLEGE LIBRARY MANAGEMENT SYSTEM - MySQL DATABASE SCHEMA
-- Database: library_db
-- ============================================================================

CREATE DATABASE IF NOT EXISTS library_db;
USE library_db;

-- ----------------------------------------------------------------------------
-- 1. BOOKS TABLE
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS books (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    isbn VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    author VARCHAR(150) NOT NULL,
    category VARCHAR(100),
    total_copies INT NOT NULL DEFAULT 1,
    available_copies INT NOT NULL DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_isbn (isbn),
    INDEX idx_title (title)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------------------------------
-- 2. STUDENTS TABLE
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS students (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    department VARCHAR(50) NOT NULL,
    section VARCHAR(10) NOT NULL,
    study_year INT NOT NULL,
    email VARCHAR(150) NOT NULL,
    barcode_code VARCHAR(50),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_student_id (student_id),
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------------------------------
-- 3. BORROW TRANSACTIONS TABLE
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS borrow_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    issue_date DATE NOT NULL,
    due_date DATE NOT NULL,
    return_date DATE NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ISSUED', -- 'ISSUED', 'RETURNED', 'OVERDUE'
    email_sent BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE,
    INDEX idx_status (status),
    INDEX idx_due_date (due_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------------------------------
-- 4. WAITLIST RESERVATIONS TABLE
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS waitlist_reservations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    reservation_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'WAITING', -- 'WAITING', 'NOTIFIED', 'FULFILLED', 'CANCELLED'
    notification_date DATE NULL,
    email_sent BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE,
    INDEX idx_res_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================================
-- INITIAL SEED DATA
-- =================================--------------------------------------------

-- Insert Books
INSERT INTO books (isbn, title, author, category, total_copies, available_copies) VALUES
('978-1-032-78600-1', 'Fundamentals of Data Science', 'Sanjeev J. Wagh, Manisha S. Bhende, Anuradha D. Thakare', 'Data Science', 5, 5),
('978-0132350884', 'Clean Code', 'Robert C. Martin', 'Software Engineering', 4, 4),
('978-0134685991', 'Effective Java', 'Joshua Bloch', 'Java Programming', 3, 3),
('978-0201633610', 'Design Patterns: Elements of Reusable Object-Oriented Software', 'Erich Gamma, Richard Helm, Ralph Johnson, John Vlissides', 'Software Architecture', 2, 2),
('978-0262033848', 'Introduction to Algorithms', 'Thomas H. Cormen', 'Computer Science', 5, 5),
('978-0133594140', 'Computer Networking: A Top-Down Approach', 'James F. Kurose, Keith W. Ross', 'Networking', 3, 3),
('978-0078022159', 'Database System Concepts', 'Abraham Silberschatz, Henry F. Korth', 'Databases', 4, 4)
ON DUPLICATE KEY UPDATE title=VALUES(title);

-- Insert Students
INSERT INTO students (student_id, name, department, section, study_year, email, barcode_code) VALUES
('310625205283', 'Priya', 'IT', 'E', 2, '310625205283@eec.srmrmp.edu.in', '310625205283'),
('EECIT001', 'Yashwanthiga', 'IT', 'E', 2, 'yashwanthiga@gmail.com', 'EECIT001'),
('EECIT002', 'Tanisha Balaji', 'IT', 'E', 2, 'tanishabalaji7@gmail.com', 'EECIT002'),
('EECIT003', 'Karthik', 'IT', 'E', 2, 'karthik@eec.srmrmp.edu.in', 'EECIT003'),
('EECIT004', 'Sindhureny', 'IT', 'E', 2, 'sindhureny@eec.srmrmp.edu.in', 'EECIT004'),
('EECIT005', 'Vishnu', 'IT', 'E', 2, 'vishnu@eec.srmrmp.edu.in', 'EECIT005'),
('EECIT006', 'Tarunshree', 'IT', 'E', 2, 'tarunshree@eec.srmrmp.edu.in', 'EECIT006')
ON DUPLICATE KEY UPDATE name=VALUES(name);
