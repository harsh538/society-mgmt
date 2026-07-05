package com.society.app.booking;

import com.society.app.booking.dto.ApproveBookingRequest;
import com.society.app.booking.dto.BookingDto;
import com.society.app.booking.dto.RejectBookingRequest;
import com.society.app.booking.dto.RequestBookingRequest;
import com.society.app.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Terrace booking endpoints (project.md § 5.8).
 */
@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<BookingDto>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(bookingService.list(status, from, to, pageable)));
    }

    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<Page<BookingDto>>> mine(
            Pageable pageable, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(
                bookingService.myBookings(authentication.getName(), pageable)));
    }

    @GetMapping("/availability")
    public ResponseEntity<ApiResponse<List<LocalDate>>> availability(
            @RequestParam int year, @RequestParam int month) {
        return ResponseEntity.ok(ApiResponse.ok(bookingService.availability(year, month)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BookingDto>> request(
            @Valid @RequestBody RequestBookingRequest req,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(
                bookingService.request(authentication.getName(), req), "Booking requested"));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BookingDto>> approve(
            @PathVariable Long id,
            @Valid @RequestBody ApproveBookingRequest req,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(
                bookingService.approve(id, authentication.getName(), req), "Booking approved"));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BookingDto>> reject(
            @PathVariable Long id,
            @RequestBody RejectBookingRequest req,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(
                bookingService.reject(id, authentication.getName(), req), "Booking rejected"));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<BookingDto>> cancel(
            @PathVariable Long id, Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
        return ResponseEntity.ok(ApiResponse.ok(
                bookingService.cancel(id, authentication.getName(), isAdmin), "Booking cancelled"));
    }
}
