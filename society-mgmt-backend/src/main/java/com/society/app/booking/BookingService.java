package com.society.app.booking;

import com.society.app.booking.dto.ApproveBookingRequest;
import com.society.app.booking.dto.BookingDto;
import com.society.app.booking.dto.RejectBookingRequest;
import com.society.app.booking.dto.RequestBookingRequest;
import com.society.app.member.Member;
import com.society.app.member.MemberRepository;
import com.society.app.unit.Unit;
import com.society.app.unit.UnitRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final MemberRepository memberRepository;
    private final UnitRepository unitRepository;

    @Transactional
    public BookingDto request(String phone, RequestBookingRequest req) {
        Member member = memberRepository.findByPhone(phone)
                .orElseThrow(() -> new AccessDeniedException("Unknown caller"));

        Unit unit = null;
        if (req.unitId() != null) {
            unit = unitRepository.findById(req.unitId())
                    .orElseThrow(() -> new EntityNotFoundException("Unit not found: " + req.unitId()));
        }

        TerraceBooking booking = new TerraceBooking();
        booking.setBookedBy(member);
        booking.setUnit(unit);
        booking.setEventTitle(req.eventTitle());
        booking.setEventDate(req.eventDate());
        booking.setStatus("REQUESTED");

        return BookingDto.from(bookingRepository.save(booking));
    }

    @Transactional
    public BookingDto approve(Long id, String adminPhone, ApproveBookingRequest req) {
        TerraceBooking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found: " + id));
        if (!"REQUESTED".equals(booking.getStatus())) {
            throw new IllegalStateException("Only REQUESTED bookings can be approved");
        }
        if (bookingRepository.existsByEventDateAndStatus(booking.getEventDate(), "APPROVED")) {
            throw new IllegalStateException(
                    "There is already an approved booking on " + booking.getEventDate());
        }

        Member admin = memberRepository.findByPhone(adminPhone)
                .orElseThrow(() -> new AccessDeniedException("Unknown caller"));

        booking.setStatus("APPROVED");
        booking.setNominalFee(req.nominalFee());
        booking.setApprovedBy(admin);

        return BookingDto.from(bookingRepository.save(booking));
    }

    @Transactional
    public BookingDto reject(Long id, String adminPhone, RejectBookingRequest req) {
        TerraceBooking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found: " + id));
        if (!"REQUESTED".equals(booking.getStatus())) {
            throw new IllegalStateException("Only REQUESTED bookings can be rejected");
        }

        Member admin = memberRepository.findByPhone(adminPhone)
                .orElseThrow(() -> new AccessDeniedException("Unknown caller"));

        booking.setStatus("REJECTED");
        booking.setApprovedBy(admin);

        return BookingDto.from(bookingRepository.save(booking));
    }

    @Transactional
    public BookingDto cancel(Long id, String phone, boolean isAdmin) {
        TerraceBooking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found: " + id));

        if (!isAdmin) {
            if (booking.getBookedBy() == null || !phone.equals(booking.getBookedBy().getPhone())) {
                throw new AccessDeniedException("Not authorized to cancel this booking");
            }
            if (!"REQUESTED".equals(booking.getStatus())) {
                throw new IllegalStateException("Members can only cancel REQUESTED bookings");
            }
        }

        booking.setStatus("CANCELLED");
        return BookingDto.from(bookingRepository.save(booking));
    }

    @Transactional(readOnly = true)
    public Page<BookingDto> list(String status, LocalDate from, LocalDate to, Pageable pageable) {
        String s = (status != null && status.isBlank()) ? null : status;
        return bookingRepository.findFiltered(s, from, to, pageable).map(BookingDto::from);
    }

    @Transactional(readOnly = true)
    public Page<BookingDto> myBookings(String phone, Pageable pageable) {
        Member m = memberRepository.findByPhone(phone)
                .orElseThrow(() -> new AccessDeniedException("Unknown caller"));
        return bookingRepository.findByBookedById(m.getId(), pageable).map(BookingDto::from);
    }

    @Transactional(readOnly = true)
    public List<LocalDate> availability(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        return bookingRepository.findApprovedDatesBetween(ym.atDay(1), ym.atEndOfMonth());
    }
}
