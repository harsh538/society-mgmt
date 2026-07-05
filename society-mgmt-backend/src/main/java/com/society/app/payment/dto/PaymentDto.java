package com.society.app.payment.dto;

import com.society.app.charge.MaintenanceCharge;
import com.society.app.member.Member;
import com.society.app.payment.Payment;
import com.society.app.receipt.Receipt;
import com.society.app.unit.Unit;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Read-side projection of {@link Payment}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDto {

    private Long id;
    private Long unitId;
    private String unitNumber;
    private Long submittedById;
    private String submittedByName;
    private Long chargeId;
    private String paymentType;
    private BigDecimal amount;
    private String method;
    private String utrReference;
    private String proofFilePath;
    private OffsetDateTime paidAt;
    private String status;
    private Long verifiedById;
    private OffsetDateTime verifiedAt;
    private String rejectionReason;
    private Long receiptId;
    private OffsetDateTime createdAt;

    public static PaymentDto from(Payment p) {
        Unit u = p.getUnit();
        Member submitter = p.getSubmittedBy();
        MaintenanceCharge charge = p.getCharge();
        Member verifier = p.getVerifiedBy();
        Receipt receipt = p.getReceipt();
        return new PaymentDto(
                p.getId(),
                u != null ? u.getId() : null,
                u != null ? u.getUnitNumber() : null,
                submitter != null ? submitter.getId() : null,
                submitter != null ? submitter.getFullName() : null,
                charge != null ? charge.getId() : null,
                p.getPaymentType(),
                p.getAmount(),
                p.getMethod(),
                p.getUtrReference(),
                p.getProofFilePath(),
                p.getPaidAt(),
                p.getStatus(),
                verifier != null ? verifier.getId() : null,
                p.getVerifiedAt(),
                p.getRejectionReason(),
                receipt != null ? receipt.getId() : null,
                p.getCreatedAt()
        );
    }
}
