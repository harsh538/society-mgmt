package com.society.app.booking;

import com.society.app.booking.dto.ApproveBookingRequest;
import com.society.app.booking.dto.BookingDto;
import com.society.app.member.Member;
import com.society.app.member.MemberRepository;
import com.society.app.unit.UnitRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for terrace-booking business rules (project.md § 7.8, § 8).
 */
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock BookingRepository bookingRepository;
    @Mock MemberRepository memberRepository;
    @Mock UnitRepository unitRepository;

    @InjectMocks BookingService bookingService;

    private Member admin;
    private Member member;

    @BeforeEach
    void setUp() {
        admin = new Member();
        admin.setId(1L);
        admin.setPhone("9999999999");
        admin.setFullName("Admin");

        member = new Member();
        member.setId(2L);
        member.setPhone("9888888888");
        member.setFullName("Member One");
    }

    // -------------------------------------------------------------------------
    // approve
    // -------------------------------------------------------------------------

    @Test
    void approve_setsStatusApproved_whenDateIsFree() {
        LocalDate date = LocalDate.of(2026, 9, 15);
        TerraceBooking booking = requestedBooking(date, member);

        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(bookingRepository.existsByEventDateAndStatus(date, "APPROVED")).thenReturn(false);
        when(memberRepository.findByPhone("9999999999")).thenReturn(Optional.of(admin));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ApproveBookingRequest req = new ApproveBookingRequest(BigDecimal.valueOf(500));
        BookingDto result = bookingService.approve(10L, "9999999999", req);

        assertThat(result.status()).isEqualTo("APPROVED");
        assertThat(result.nominalFee()).isEqualByComparingTo(BigDecimal.valueOf(500));
    }

    @Test
    void approve_throwsIllegalState_whenDateAlreadyTaken() {
        LocalDate date = LocalDate.of(2026, 9, 15);
        TerraceBooking booking = requestedBooking(date, member);

        when(bookingRepository.findById(11L)).thenReturn(Optional.of(booking));
        when(bookingRepository.existsByEventDateAndStatus(date, "APPROVED")).thenReturn(true);

        ApproveBookingRequest req = new ApproveBookingRequest(BigDecimal.ZERO);

        assertThatThrownBy(() -> bookingService.approve(11L, "9999999999", req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already an approved booking");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void approve_throwsIllegalState_whenBookingNotRequested() {
        TerraceBooking booking = requestedBooking(LocalDate.now().plusDays(1), member);
        booking.setStatus("APPROVED");

        when(bookingRepository.findById(12L)).thenReturn(Optional.of(booking));

        ApproveBookingRequest req = new ApproveBookingRequest(BigDecimal.ZERO);

        assertThatThrownBy(() -> bookingService.approve(12L, "9999999999", req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only REQUESTED");
    }

    // -------------------------------------------------------------------------
    // cancel (member)
    // -------------------------------------------------------------------------

    @Test
    void cancel_allowsMemberToCancelOwnRequestedBooking() {
        TerraceBooking booking = requestedBooking(LocalDate.now().plusDays(3), member);

        when(bookingRepository.findById(20L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BookingDto result = bookingService.cancel(20L, "9888888888", false);

        assertThat(result.status()).isEqualTo("CANCELLED");
    }

    @Test
    void cancel_throwsAccessDenied_whenMemberCancelsOthersBooking() {
        TerraceBooking booking = requestedBooking(LocalDate.now().plusDays(3), member);

        when(bookingRepository.findById(21L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancel(21L, "9777777777", false))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void cancel_throwsIllegalState_whenMemberCancelsApprovedBooking() {
        TerraceBooking booking = requestedBooking(LocalDate.now().plusDays(3), member);
        booking.setStatus("APPROVED");

        when(bookingRepository.findById(22L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancel(22L, "9888888888", false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("only cancel REQUESTED");
    }

    @Test
    void cancel_adminCanCancelAnyBooking() {
        TerraceBooking booking = requestedBooking(LocalDate.now().plusDays(3), member);
        booking.setStatus("APPROVED");

        when(bookingRepository.findById(23L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BookingDto result = bookingService.cancel(23L, "9999999999", true);

        assertThat(result.status()).isEqualTo("CANCELLED");
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private TerraceBooking requestedBooking(LocalDate date, Member bookedBy) {
        TerraceBooking b = new TerraceBooking();
        b.setId(10L);
        b.setEventTitle("Birthday Party");
        b.setEventDate(date);
        b.setStatus("REQUESTED");
        b.setNominalFee(BigDecimal.ZERO);
        b.setBookedBy(bookedBy);
        return b;
    }
}
