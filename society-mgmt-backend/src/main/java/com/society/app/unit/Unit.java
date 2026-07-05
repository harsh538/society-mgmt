package com.society.app.unit;

import com.society.app.member.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
 * Unit entity — a FLAT, SHOP, or TERRACE in the society (project.md § 3.2).
 *
 * <p><strong>Charge amount rule:</strong>
 * {@code amount_due = base_maintenance + (occupancy='TENANT' ? tenant_surcharge : 0)}.
 * The fields are stored on the unit; the calculation happens in Phase 4 charge generation.</p>
 *
 * <p>TERRACE units have no owner and are excluded from monthly maintenance generation.</p>
 */
@Entity
@Table(name = "units")
@Getter
@Setter
@NoArgsConstructor
public class Unit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "unit_type", nullable = false, length = 20)
    private String unitType;          // FLAT | SHOP | TERRACE

    @Column(name = "unit_number", nullable = false, unique = true, length = 30)
    private String unitNumber;

    @Column(name = "floor", length = 20)
    private String floor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_member_id")
    private Member owner;

    @Column(name = "occupancy", nullable = false, length = 20)
    private String occupancy;         // OWNER | TENANT | VACANT

    @Column(name = "base_maintenance", nullable = false, precision = 12, scale = 2)
    private BigDecimal baseMaintenance = BigDecimal.ZERO;

    @Column(name = "tenant_surcharge", nullable = false, precision = 12, scale = 2)
    private BigDecimal tenantSurcharge = BigDecimal.ZERO;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

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
