package com.society.app.charge.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of an idempotent monthly charge run.
 * {@code created} = newly inserted charges, {@code skipped} = pre-existing rows
 * for the same (unit, year, month) tuple, {@code total} = created + skipped.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenerateChargesResponse {

    private int created;
    private int skipped;
    private int total;
}
