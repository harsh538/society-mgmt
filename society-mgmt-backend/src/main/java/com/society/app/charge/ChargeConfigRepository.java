package com.society.app.charge;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for the singleton {@link ChargeConfig} row. Callers always use
 * {@code findById(1L).orElseThrow(...)}.
 */
@Repository
public interface ChargeConfigRepository extends JpaRepository<ChargeConfig, Long> {
}
