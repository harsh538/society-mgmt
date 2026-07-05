package com.society.app.expense;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<SocietyExpense, Long> {

    @Query("""
            SELECT e FROM SocietyExpense e
            WHERE e.deleted = false
              AND (:category IS NULL OR e.category = :category)
              AND (:from IS NULL OR e.expenseDate >= :from)
              AND (:to IS NULL OR e.expenseDate <= :to)
            ORDER BY e.expenseDate DESC, e.id DESC
            """)
    Page<SocietyExpense> findFiltered(
            @Param("category") String category,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable);

    @Query("""
            SELECT e.category as category, SUM(e.amount) as total
            FROM SocietyExpense e
            WHERE e.deleted = false
              AND (:from IS NULL OR e.expenseDate >= :from)
              AND (:to IS NULL OR e.expenseDate <= :to)
            GROUP BY e.category
            ORDER BY total DESC
            """)
    List<CategoryTotal> findCategoryTotals(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM SocietyExpense e WHERE e.deleted = false")
    BigDecimal sumAllExpenses();

    interface CategoryTotal {
        String getCategory();
        BigDecimal getTotal();
    }
}
