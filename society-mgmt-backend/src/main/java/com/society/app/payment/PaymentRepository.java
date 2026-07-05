package com.society.app.payment;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Page<Payment> findByStatus(String status, Pageable pageable);

    Page<Payment> findByUnitId(Long unitId, Pageable pageable);

    Page<Payment> findByUnitIdAndStatus(Long unitId, String status, Pageable pageable);

    Page<Payment> findBySubmittedById(Long memberId, Pageable pageable);

    /** Dashboard "Pending verifications" badge (project.md § 5.9 financials). */
    long countByStatus(String status);

    /**
     * Pessimistic lock used by {@code PaymentService.verifyPayment} (project.md § 5.5):
     * "Load payment (must be PENDING) FOR UPDATE".
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.id = :id")
    Optional<Payment> findByIdForUpdate(@Param("id") Long id);
}
