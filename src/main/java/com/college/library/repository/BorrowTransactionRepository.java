package com.college.library.repository;

import com.college.library.entity.BorrowTransaction;
import com.college.library.entity.Student;
import com.college.library.entity.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BorrowTransactionRepository extends JpaRepository<BorrowTransaction, Long> {
    List<BorrowTransaction> findByStudentOrderByIssueDateDesc(Student student);
    List<BorrowTransaction> findByStatusOrderByIssueDateDesc(TransactionStatus status);
    List<BorrowTransaction> findAllByOrderByIdDesc();
    long countByStatus(TransactionStatus status);
}
