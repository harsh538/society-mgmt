package com.society.app.expense;

import com.society.app.expense.dto.CategoryTotalDto;
import com.society.app.expense.dto.CreateExpenseRequest;
import com.society.app.expense.dto.ExpenseDto;
import com.society.app.expense.dto.ExpenseSummaryDto;
import com.society.app.expense.dto.UpdateExpenseRequest;
import com.society.app.member.Member;
import com.society.app.member.MemberRepository;
import com.society.app.storage.FileStorageService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final MemberRepository memberRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public ExpenseDto create(String adminPhone, CreateExpenseRequest req, MultipartFile billFile) {
        Member admin = memberRepository.findByPhone(adminPhone)
                .orElseThrow(() -> new AccessDeniedException("Unknown caller"));

        String billPath = null;
        if (billFile != null && !billFile.isEmpty()) {
            billPath = fileStorageService.store(billFile, "bills");
        }

        SocietyExpense expense = new SocietyExpense();
        expense.setCategory(req.category());
        expense.setTitle(req.title());
        expense.setVendorName(req.vendorName());
        expense.setAmount(req.amount());
        expense.setExpenseDate(req.expenseDate());
        expense.setPaidFrom(req.paidFrom());
        expense.setBillFilePath(billPath);
        expense.setNote(req.note());
        expense.setRecordedBy(admin);

        return ExpenseDto.from(expenseRepository.save(expense));
    }

    @Transactional
    public ExpenseDto update(Long id, UpdateExpenseRequest req, MultipartFile billFile) {
        SocietyExpense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Expense not found: " + id));

        expense.setCategory(req.category());
        expense.setTitle(req.title());
        expense.setVendorName(req.vendorName());
        expense.setAmount(req.amount());
        expense.setExpenseDate(req.expenseDate());
        expense.setPaidFrom(req.paidFrom());
        expense.setNote(req.note());

        if (billFile != null && !billFile.isEmpty()) {
            expense.setBillFilePath(fileStorageService.store(billFile, "bills"));
        }

        return ExpenseDto.from(expenseRepository.save(expense));
    }

    @Transactional
    public void delete(Long id) {
        SocietyExpense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Expense not found: " + id));
        expense.setDeleted(true);
        expenseRepository.save(expense);
    }

    @Transactional(readOnly = true)
    public Page<ExpenseDto> list(String category, LocalDate from, LocalDate to, Pageable pageable) {
        String cat = (category != null && category.isBlank()) ? null : category;
        return expenseRepository.findFiltered(cat, from, to, pageable).map(ExpenseDto::from);
    }

    @Transactional(readOnly = true)
    public ExpenseDto get(Long id) {
        SocietyExpense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Expense not found: " + id));
        if (expense.isDeleted()) {
            throw new EntityNotFoundException("Expense not found: " + id);
        }
        return ExpenseDto.from(expense);
    }

    @Transactional(readOnly = true)
    public ExpenseSummaryDto summary(LocalDate from, LocalDate to) {
        List<ExpenseRepository.CategoryTotal> rows = expenseRepository.findCategoryTotals(from, to);
        BigDecimal grand = rows.stream()
                .map(ExpenseRepository.CategoryTotal::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<CategoryTotalDto> byCategory = rows.stream()
                .map(r -> new CategoryTotalDto(r.getCategory(), r.getTotal()))
                .toList();
        return new ExpenseSummaryDto(grand, byCategory);
    }
}
