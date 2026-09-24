package com.college.library.repository;

import com.college.library.entity.WaitlistReservation;
import com.college.library.entity.WaitlistStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WaitlistReservationRepository extends JpaRepository<WaitlistReservation, Long> {

    List<WaitlistReservation> findByBookIdAndStatusOrderByReservationDateAsc(Long bookId, WaitlistStatus status);

    List<WaitlistReservation> findByStudentIdOrderByReservationDateDesc(Long studentId);

    Optional<WaitlistReservation> findByBookIdAndStudentIdAndStatus(Long bookId, Long studentId, WaitlistStatus status);

    List<WaitlistReservation> findByStatusOrderByReservationDateDesc(WaitlistStatus status);

    List<WaitlistReservation> findAllByOrderByReservationDateDesc();
}
