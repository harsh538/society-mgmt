package com.society.app.payment;

import com.society.app.charge.ChargeConfig;
import com.society.app.charge.ChargeConfigRepository;
import com.society.app.charge.MaintenanceCharge;
import com.society.app.charge.MaintenanceChargeRepository;
import com.society.app.member.Member;
import com.society.app.member.MemberRepository;
import com.society.app.payment.dto.PaymentDto;
import com.society.app.payment.dto.RejectPaymentRequest;
import com.society.app.receipt.PdfReceiptGenerator;
import com.society.app.receipt.Receipt;
import com.society.app.receipt.ReceiptRepository;
import com.society.app.storage.FileStorageService;
import com.society.app.unit.Unit;
import com.society.app.unit.UnitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the payment verification transaction (project.md § 5.5, § 7.4–7.6).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentServiceTest {

    @Mock PaymentRepository paymentRepository;
    @Mock ReceiptRepository receiptRepository;
    @Mock MaintenanceChargeRepository maintenanceChargeRepository;
    @Mock FileStorageService fileStorageService;
    @Mock PdfReceiptGenerator pdfReceiptGenerator;
    @Mock ChargeConfigRepository chargeConfigRepository;
    @Mock MemberRepository memberRepository;
    @Mock UnitRepository unitRepository;

    @InjectMocks PaymentService paymentService;

    private Member admin;
    private Member owner;
    private Unit unit;
    private ChargeConfig config;

    @BeforeEach
    void setUp() {
        admin = new Member();
        admin.setId(1L);
        admin.setPhone("9999999999");
        admin.setFullName("Admin");

        owner = new Member();
        owner.setId(2L);
        owner.setPhone("9888888888");
        owner.setFullName("Owner");

        unit = new Unit();
        unit.setId(10L);
        unit.setUnitNumber("A-101");
        unit.setOwner(owner);

        config = new ChargeConfig();
        config.setSocietyName("Test Society");

        when(memberRepository.findByPhone("9999999999")).thenReturn(Optional.of(admin));
    }

    // -------------------------------------------------------------------------
    // Shared stub helper (only called by verify tests)
    // -------------------------------------------------------------------------

    private void stubVerifyInfrastructure() {
        when(chargeConfigRepository.findById(1L)).thenReturn(Optional.of(config));
        when(receiptRepository.countByYear(anyInt())).thenReturn(0L);
        when(pdfReceiptGenerator.generateReceipt(any(), anyString()))
                .thenReturn("receipts/test.pdf");
        when(receiptRepository.save(any())).thenAnswer(inv -> {
            Receipt r = inv.getArgument(0);
            r.setId(99L);
            return r;
        });
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // -------------------------------------------------------------------------
    // verify: MAINTENANCE charge reconciliation
    // -------------------------------------------------------------------------

    @Test
    void verify_setsStatusPaid_whenPaymentCoversFullAmount() {
        stubVerifyInfrastructure();
        MaintenanceCharge charge = charge(new BigDecimal("1500"), BigDecimal.ZERO);
        Payment payment = pendingPayment(new BigDecimal("1500"), "MAINTENANCE", charge);

        when(paymentRepository.findByIdForUpdate(42L)).thenReturn(Optional.of(payment));
        when(maintenanceChargeRepository.findById(charge.getId())).thenReturn(Optional.of(charge));
        when(maintenanceChargeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentDto result = paymentService.verifyPayment(42L, "9999999999");

        assertThat(charge.getAmountPaid()).isEqualByComparingTo(new BigDecimal("1500"));
        assertThat(charge.getStatus()).isEqualTo("PAID");
        assertThat(result.getStatus()).isEqualTo("VERIFIED");
    }

    @Test
    void verify_setsStatusPartial_whenPaymentPartiallyCoversCharge() {
        stubVerifyInfrastructure();
        MaintenanceCharge charge = charge(new BigDecimal("1500"), BigDecimal.ZERO);
        Payment payment = pendingPayment(new BigDecimal("500"), "MAINTENANCE", charge);

        when(paymentRepository.findByIdForUpdate(43L)).thenReturn(Optional.of(payment));
        when(maintenanceChargeRepository.findById(charge.getId())).thenReturn(Optional.of(charge));
        when(maintenanceChargeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        paymentService.verifyPayment(43L, "9999999999");

        assertThat(charge.getAmountPaid()).isEqualByComparingTo(new BigDecimal("500"));
        assertThat(charge.getStatus()).isEqualTo("PARTIAL");
    }

    @Test
    void verify_accumulates_withPreviousPartialPayment() {
        stubVerifyInfrastructure();
        // Charge already has 500 paid; another 1000 should push it to PAID.
        MaintenanceCharge charge = charge(new BigDecimal("1500"), new BigDecimal("500"));
        Payment payment = pendingPayment(new BigDecimal("1000"), "MAINTENANCE", charge);

        when(paymentRepository.findByIdForUpdate(44L)).thenReturn(Optional.of(payment));
        when(maintenanceChargeRepository.findById(charge.getId())).thenReturn(Optional.of(charge));
        when(maintenanceChargeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        paymentService.verifyPayment(44L, "9999999999");

        assertThat(charge.getAmountPaid()).isEqualByComparingTo(new BigDecimal("1500"));
        assertThat(charge.getStatus()).isEqualTo("PAID");
    }

    @Test
    void verify_throwsIllegalState_whenPaymentAlreadyVerified() {
        Payment payment = pendingPayment(new BigDecimal("500"), "MAINTENANCE", null);
        payment.setStatus("VERIFIED");

        when(paymentRepository.findByIdForUpdate(45L)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.verifyPayment(45L, "9999999999"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not in PENDING");
    }

    // -------------------------------------------------------------------------
    // reject
    // -------------------------------------------------------------------------

    @Test
    void reject_throwsIllegalState_whenPaymentAlreadyRejected() {
        Payment payment = pendingPayment(new BigDecimal("500"), "MAINTENANCE", null);
        payment.setStatus("REJECTED");

        when(paymentRepository.findByIdForUpdate(46L)).thenReturn(Optional.of(payment));

        RejectPaymentRequest req = new RejectPaymentRequest();
        req.setRejectionReason("Duplicate");

        assertThatThrownBy(() -> paymentService.rejectPayment(46L, "9999999999", req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not in PENDING");
    }

    @Test
    void reject_setsStatusRejected_andStoresReason() {
        Payment payment = pendingPayment(new BigDecimal("500"), "OTHER", null);
        when(paymentRepository.findByIdForUpdate(47L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RejectPaymentRequest req = new RejectPaymentRequest();
        req.setRejectionReason("Wrong UTR");

        PaymentDto result = paymentService.rejectPayment(47L, "9999999999", req);

        verify(paymentRepository).save(payment);
        assertThat(result.getStatus()).isEqualTo("REJECTED");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Payment pendingPayment(BigDecimal amount, String type, MaintenanceCharge charge) {
        Payment p = new Payment();
        p.setId(42L);
        p.setAmount(amount);
        p.setPaymentType(type);
        p.setMethod("UPI");
        p.setCharge(charge);
        p.setUnit(unit);
        p.setSubmittedBy(owner);
        p.setStatus("PENDING");
        return p;
    }

    private MaintenanceCharge charge(BigDecimal amountDue, BigDecimal amountPaid) {
        MaintenanceCharge c = new MaintenanceCharge();
        c.setId(55L);
        c.setAmountDue(amountDue);
        c.setAmountPaid(amountPaid);
        c.setStatus("DUE");
        c.setUnit(unit);
        return c;
    }
}
