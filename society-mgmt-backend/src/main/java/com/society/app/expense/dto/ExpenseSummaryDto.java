package com.society.app.expense.dto;

import java.math.BigDecimal;
import java.util.List;

public record ExpenseSummaryDto(BigDecimal grandTotal, List<CategoryTotalDto> byCategory) {}
