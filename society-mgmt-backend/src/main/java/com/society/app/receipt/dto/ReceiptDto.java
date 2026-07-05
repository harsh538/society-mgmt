package com.society.app.receipt.dto;

import com.society.app.member.Member;
import com.society.app.payment.Payment;
import com.society.app.receipt.Receipt;
import com.society.app.unit.Unit;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptDto {

    private Long id;
    private String receiptNumber;
    private Long paymentId;
    private Long unitId;
    private String unitNumber;
    private Long issuedToId;
    private String issuedToName;
    private BigDecimal amount;
    private OffsetDateTime issuedAt;
    private String pdfFilePath;

    public static ReceiptDto from(Receipt r) {
        Unit u = r.getUnit();
        Member issued = r.getIssuedTo();
        Payment p = r.getPayment();
        return new ReceiptDto(
                r.getId(),
                r.getReceiptNumber(),
                p != null ? p.getId() : null,
                u != null ? u.getId() : null,
                u != null ? u.getUnitNumber() : null,
                issued != null ? issued.getId() : null,
                issued != null ? issued.getFullName() : null,
                r.getAmount(),
                r.getIssuedAt(),
                r.getPdfFilePath()
        );
    }
}
