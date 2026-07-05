package com.society.app.payment;

import com.society.app.charge.ChargeConfig;
import com.society.app.charge.ChargeConfigRepository;
import com.society.app.charge.MaintenanceCharge;
import com.society.app.charge.MaintenanceChargeRepository;
import com.society.app.member.Member;
import com.society.app.member.MemberRepository;
import com.society.app.payment.dto.PaymentDto;
import com.society.app.payment.dto.RejectPaymentRequest;
import com.society.app.payment.dto.SubmitPaymentRequest;
import com.society.app.receipt.PdfReceiptGenerator;
import com.society.app.receipt.PdfReceiptGenerator.ReceiptPdfData;
import com.society.app.receipt.Receipt;
import com.society.app.receipt.ReceiptRepository;
import com.society.app.storage.FileStorageService;
import com.society.app.unit.Unit;
import com.society.app.unit.UnitRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Manual payment-verification flow (project.md § 5.5, § 7.4, § 7.5).
 *
 * <p>Verification is the authoritative money-mutation path:
 * <ol>
 *   <li>Lock the payment row {@code FOR UPDATE} (must be PENDING).</li>
 *   <li>If MAINTENANCE + charge_id present: update {@code amount_paid} + recompute status.</li>
 *   <li>Generate the next receipt number for the calendar year.</li>
 *   <li>Insert the receipt row, render the PDF, store the path.</li>
 *   <li>Set payment.status=VERIFIED, verified_by, verified_at, receipt_id. Commit.</li>
 * </ol>
 * Any failure rolls back the entire transaction; the payment stays PENDING.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private static final Long CONFIG_ID = 1L;

    private final PaymentRepository paymentRepository;
    private final ReceiptRepository receiptRepository;
    private final MaintenanceChargeRepository maintenanceChargeRepository;
    private final FileStorageService fileStorageService;
    private final PdfReceiptGenerator pdfReceiptGenerator;
    private final ChargeConfigRepository chargeConfigRepository;
    private final MemberRepository memberRepository;
    private final UnitRepository unitRepository;

    @Value("${app.receipt-prefix:RCP}")
    private String receiptPrefix;

    // -------------------------------------------------------------------------
    // Submit (MEMBER)
    // -------------------------------------------------------------------------

    @Transactional
    public PaymentDto submitPayment(String phone, SubmitPaymentRequest req, MultipartFile proofFile) {
        Member submitter = memberRepository.findByPhone(phone)
                .orElseThrow(() -> new AccessDeniedException("Unknown caller"));

        Unit unit = unitRepository.findById(req.getUnitId())
                .orElseThrow(() -> new EntityNotFoundException("Unit not found: " + req.getUnitId()));
        if (!unit.isActive()) {
            throw new IllegalArgumentException("Unit is inactive");
        }

        MaintenanceCharge charge = null;
        if (req.getChargeId() != null) {
            charge = maintenanceChargeRepository.findById(req.getChargeId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Charge not found: " + req.getChargeId()));
            if (!charge.getUnit().getId().equals(unit.getId())) {
                throw new IllegalArgumentException(
                        "Charge does not belong to the supplied unit");
            }
        }

        String proofPath = null;
        if (proofFile != null && !proofFile.isEmpty()) {
            proofPath = fileStorageService.store(proofFile, "proofs");
        }

        Payment p = new Payment();
        p.setUnit(unit);
        p.setSubmittedBy(submitter);
        p.setCharge(charge);
        p.setPaymentType(req.getPaymentType());
        p.setAmount(req.getAmount());
        p.setMethod(req.getMethod());
        p.setUtrReference(req.getUtrReference());
        p.setProofFilePath(proofPath);
        p.setPaidAt(req.getPaidAt());
        p.setStatus("PENDING");

        Payment saved = paymentRepository.save(p);
        return PaymentDto.from(saved);
    }

    // -------------------------------------------------------------------------
    // Verify (ADMIN) — authoritative transaction
    // -------------------------------------------------------------------------

    @Transactional
    public PaymentDto verifyPayment(Long paymentId, String adminPhone) {
        // Step 1 — pessimistic lock; payment must be PENDING.
        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found: " + paymentId));
        if (!"PENDING".equals(payment.getStatus())) {
            throw new IllegalStateException("Payment is not in PENDING status");
        }

        Member admin = memberRepository.findByPhone(adminPhone)
                .orElseThrow(() -> new AccessDeniedException("Unknown caller"));

        // Step 2 — reconcile against the maintenance charge if applicable.
        if ("MAINTENANCE".equals(payment.getPaymentType()) && payment.getCharge() != null) {
            MaintenanceCharge charge = maintenanceChargeRepository
                    .findById(payment.getCharge().getId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Charge not found: " + payment.getCharge().getId()));
            if ("VOID".equals(charge.getStatus())) {
                throw new IllegalStateException(
                        "Cannot apply payment against a voided charge");
            }
            BigDecimal currentPaid = charge.getAmountPaid() == null
                    ? BigDecimal.ZERO : charge.getAmountPaid();
            BigDecimal newPaid = currentPaid.add(payment.getAmount());
            charge.setAmountPaid(newPaid);

            BigDecimal due = charge.getAmountDue() == null
                    ? BigDecimal.ZERO : charge.getAmountDue();
            String newStatus;
            if (newPaid.compareTo(due) >= 0) {
                newStatus = "PAID";
            } else if (newPaid.compareTo(BigDecimal.ZERO) > 0) {
                newStatus = "PARTIAL";
            } else {
                newStatus = "DUE";
            }
            charge.setStatus(newStatus);
            maintenanceChargeRepository.save(charge);
        }

        // Step 3 — generate receipt number per calendar year.
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        int year = now.getYear();
        long seq = receiptRepository.countByYear(year) + 1;
        String receiptNumber = String.format("%s-%d-%06d", receiptPrefix, year, seq);

        // Step 4 — create receipt row (saved first, then PDF generated).
        Member issuedTo = payment.getUnit().getOwner() != null
                ? payment.getUnit().getOwner()
                : payment.getSubmittedBy();

        Receipt receipt = new Receipt();
        receipt.setReceiptNumber(receiptNumber);
        receipt.setPayment(payment);
        receipt.setUnit(payment.getUnit());
        receipt.setIssuedTo(issuedTo);
        receipt.setAmount(payment.getAmount());
        receipt.setIssuedAt(now);
        receipt = receiptRepository.save(receipt);

        ChargeConfig config = chargeConfigRepository.findById(CONFIG_ID)
                .orElseThrow(() -> new IllegalStateException(
                        "Charge config singleton missing (id=1)"));

        ReceiptPdfData pdfData = new ReceiptPdfData(
                receiptNumber,
                payment.getUnit().getUnitNumber(),
                issuedTo.getFullName(),
                payment.getAmount(),
                payment.getMethod(),
                payment.getUtrReference(),
                payment.getPaidAt(),
                receipt.getIssuedAt(),
                config.getSocietyName()
        );
        String pdfPath = pdfReceiptGenerator.generateReceipt(pdfData, "receipts");
        receipt.setPdfFilePath(pdfPath);
        receiptRepository.save(receipt);

        // Step 5 — flip payment to VERIFIED and stamp the receipt FK.
        payment.setStatus("VERIFIED");
        payment.setVerifiedBy(admin);
        payment.setVerifiedAt(now);
        payment.setReceipt(receipt);
        paymentRepository.save(payment);

        log.info("Payment {} verified by {} → receipt {}",
                paymentId, adminPhone, receiptNumber);
        return PaymentDto.from(payment);
    }

    // -------------------------------------------------------------------------
    // Reject (ADMIN)
    // -------------------------------------------------------------------------

    @Transactional
    public PaymentDto rejectPayment(Long paymentId, String adminPhone, RejectPaymentRequest req) {
        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found: " + paymentId));
        if (!"PENDING".equals(payment.getStatus())) {
            throw new IllegalStateException("Payment is not in PENDING status");
        }
        Member admin = memberRepository.findByPhone(adminPhone)
                .orElseThrow(() -> new AccessDeniedException("Unknown caller"));

        payment.setStatus("REJECTED");
        payment.setRejectionReason(req.getRejectionReason());
        payment.setVerifiedBy(admin);
        payment.setVerifiedAt(OffsetDateTime.now(ZoneOffset.UTC));
        paymentRepository.save(payment);

        return PaymentDto.from(payment);
    }

    // -------------------------------------------------------------------------
    // Reads
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<PaymentDto> listPayments(Long unitId, String status, Pageable pageable) {
        boolean hasUnit = unitId != null;
        boolean hasStatus = status != null && !status.isBlank();

        Page<Payment> page;
        if (hasUnit && hasStatus) {
            page = paymentRepository.findByUnitIdAndStatus(unitId, status, pageable);
        } else if (hasUnit) {
            page = paymentRepository.findByUnitId(unitId, pageable);
        } else if (hasStatus) {
            page = paymentRepository.findByStatus(status, pageable);
        } else {
            page = paymentRepository.findAll(pageable);
        }
        return page.map(PaymentDto::from);
    }

    @Transactional(readOnly = true)
    public Page<PaymentDto> getPendingQueue(Pageable pageable) {
        Pageable oldestFirst = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by("createdAt").ascending());
        return paymentRepository.findByStatus("PENDING", oldestFirst).map(PaymentDto::from);
    }

    @Transactional(readOnly = true)
    public PaymentDto getPayment(Long id, String phone, boolean isAdmin) {
        Payment p = paymentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found: " + id));
        if (!isAdmin) {
            ensureMemberOwnsPayment(p, phone);
        }
        return PaymentDto.from(p);
    }

    @Transactional(readOnly = true)
    public Page<PaymentDto> getMyPayments(String phone, Pageable pageable) {
        Member m = memberRepository.findByPhone(phone)
                .orElseThrow(() -> new AccessDeniedException("Unknown caller"));
        Pageable newestFirst = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by("createdAt").descending());
        return paymentRepository.findBySubmittedById(m.getId(), newestFirst).map(PaymentDto::from);
    }

    @Transactional(readOnly = true)
    public String getProof(Long id, String phone, boolean isAdmin) {
        Payment p = paymentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found: " + id));
        if (!isAdmin) {
            ensureMemberOwnsPayment(p, phone);
        }
        if (p.getProofFilePath() == null || p.getProofFilePath().isBlank()) {
            throw new EntityNotFoundException("No proof attached to payment " + id);
        }
        return fileStorageService.getPublicUrl(p.getProofFilePath());
    }

    @Transactional(readOnly = true)
    public Payment loadPaymentOrThrow(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found: " + id));
    }

    private void ensureMemberOwnsPayment(Payment p, String phone) {
        if (p.getSubmittedBy() == null || !phone.equals(p.getSubmittedBy().getPhone())) {
            throw new AccessDeniedException("Member is not authorized to view this payment");
        }
    }
}
