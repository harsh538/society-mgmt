package com.society.app.expense;

import com.society.app.common.ApiResponse;
import com.society.app.expense.dto.CreateExpenseRequest;
import com.society.app.expense.dto.ExpenseDto;
import com.society.app.expense.dto.ExpenseSummaryDto;
import com.society.app.expense.dto.UpdateExpenseRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

/**
 * Society expenses endpoints (project.md § 5.7).
 * GET endpoints are open to both ADMIN and MEMBER; writes require ADMIN.
 */
@RestController
@RequestMapping("/api/v1/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ExpenseDto>>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(expenseService.list(category, from, to, pageable)));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<ExpenseSummaryDto>> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.ok(expenseService.summary(from, to)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ExpenseDto>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(expenseService.get(id)));
    }

    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ExpenseDto>> create(
            @Valid @RequestPart("expense") CreateExpenseRequest req,
            @RequestPart(value = "bill", required = false) MultipartFile billFile,
            Authentication authentication) {
        ExpenseDto data = expenseService.create(authentication.getName(), req, billFile);
        return ResponseEntity.ok(ApiResponse.ok(data, "Expense recorded"));
    }

    @PutMapping(value = "/{id}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ExpenseDto>> update(
            @PathVariable Long id,
            @Valid @RequestPart("expense") UpdateExpenseRequest req,
            @RequestPart(value = "bill", required = false) MultipartFile billFile) {
        return ResponseEntity.ok(ApiResponse.ok(expenseService.update(id, req, billFile), "Expense updated"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        expenseService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Expense deleted"));
    }
}
