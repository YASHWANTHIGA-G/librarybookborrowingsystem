package com.college.library.repository;

import com.college.library.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByStudentIdIgnoreCase(String studentId);
    Optional<Student> findByBarcode(String barcode);
    Optional<Student> findByEmailIgnoreCase(String email);
    boolean existsByStudentIdIgnoreCase(String studentId);
    boolean existsByEmailIgnoreCase(String email);
}
