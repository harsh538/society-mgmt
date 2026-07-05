package com.society.app.payment;

import com.society.app.charge.MaintenanceCharge;
import com.society.app.member.Member;
import com.society.app.receipt.Receipt;
import com.society.app.unit.Unit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Payment entity (project.md § 3.5) — member-submitted, manually verified.
 *
 * <p>Lifecycle:
 * <ol>
 *   <li>Member submits (status=PENDING).</li>
 *   <li>Admin verifies → status=VERIFIED, receipt generated, charge reconciled.</li>
 *   <li>Admin rejects → status=REJECTED, member may resubmit (new row).</li>
 * </ol>
 * Verification is fully transactional (project.md § 5.5).</p>
 */
@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by", nullable = false)
    private Member submittedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "charge_id")
    private MaintenanceCharge charge;

    @Column(name = "payment_type", nullable = false, length = 20)
    private String paymentType;   // MAINTENANCE | TERRACE_BOOKING | OTHER

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "method", nullable = false, length = 20)
    private String method;        // UPI | BANK_TRANSFER | CASH

    @Column(name = "utr_reference", length = 50)
    private String utrReference;

    @Column(name = "proof_file_path", length = 500)
    private String proofFilePath;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";  // PENDING | VERIFIED | REJECTED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    private Member verifiedBy;

    @Column(name = "verified_at")
    private OffsetDateTime verifiedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_id")
    private Receipt receipt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
