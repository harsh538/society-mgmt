package com.society.app.charge;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Society-wide configuration singleton (project.md § 3.9).
 *
 * <p>Always loaded via {@code findById(1L)}; the seed migration
 * (V1__baseline.sql) inserts the single row.</p>
 */
@Entity
@Table(name = "charge_config")
@Getter
@Setter
@NoArgsConstructor
public class ChargeConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "default_flat_maintenance", nullable = false, precision = 12, scale = 2)
    private BigDecimal defaultFlatMaintenance = BigDecimal.ZERO;

    @Column(name = "default_shop_maintenance", nullable = false, precision = 12, scale = 2)
    private BigDecimal defaultShopMaintenance = BigDecimal.ZERO;

    @Column(name = "default_tenant_surcharge", nullable = false, precision = 12, scale = 2)
    private BigDecimal defaultTenantSurcharge = BigDecimal.ZERO;

    @Column(name = "default_due_day", nullable = false)
    private int defaultDueDay = 10;

    @Column(name = "society_name", nullable = false, length = 150)
    private String societyName;

    @Column(name = "society_upi_id", length = 100)
    private String societyUpiId;

    @Column(name = "society_bank_details", length = 500)
    private String societyBankDetails;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
