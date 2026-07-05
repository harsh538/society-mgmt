package com.society.app.receipt;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, Long> {

    Page<Receipt> findByIssuedToId(Long memberId, Pageable pageable);

    Optional<Receipt> findByPaymentId(Long paymentId);

    /**
     * Sequential receipt-number generation per calendar year
     * (project.md § 7.7). Uses Postgres EXTRACT via a native query so that
     * Hibernate's portable EXTRACT works against the {@code TIMESTAMPTZ} column.
     */
    @Query(value = "SELECT COUNT(*) FROM receipts WHERE EXTRACT(YEAR FROM issued_at) = :year",
            nativeQuery = true)
    long countByYear(@Param("year") int year);
}
