-- =============================================================================
-- V1__baseline.sql
-- Society Management Application — initial schema.
-- All 9 tables per project.md § 3. Money columns NUMERIC(12,2). PKs IDENTITY.
-- All timestamps TIMESTAMPTZ default now(). UTF8 / UTC assumed.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 3.1 members
-- -----------------------------------------------------------------------------
CREATE TABLE members (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    full_name       VARCHAR(150) NOT NULL,
    phone           VARCHAR(15)  NOT NULL UNIQUE,
    email           VARCHAR(150) UNIQUE,
    password_hash   VARCHAR(255),
    role            VARCHAR(20)  NOT NULL DEFAULT 'MEMBER'
                    CHECK (role IN ('ADMIN','MEMBER')),
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    last_login_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_members_role ON members(role);
CREATE INDEX idx_members_is_active ON members(is_active);

-- -----------------------------------------------------------------------------
-- 3.9 charge_config (singleton)
-- -----------------------------------------------------------------------------
CREATE TABLE charge_config (
    id                          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    default_flat_maintenance    NUMERIC(12,2) NOT NULL DEFAULT 0,
    default_shop_maintenance    NUMERIC(12,2) NOT NULL DEFAULT 0,
    default_tenant_surcharge    NUMERIC(12,2) NOT NULL DEFAULT 100,
    default_due_day             INT           NOT NULL DEFAULT 10
                                CHECK (default_due_day BETWEEN 1 AND 28),
    society_name                VARCHAR(150)  NOT NULL,
    society_upi_id              VARCHAR(100),
    society_bank_details        VARCHAR(500),
    created_at                  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- -----------------------------------------------------------------------------
-- 3.2 units
-- -----------------------------------------------------------------------------
CREATE TABLE units (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    unit_type           VARCHAR(20)  NOT NULL
                        CHECK (unit_type IN ('FLAT','SHOP','TERRACE')),
    unit_number         VARCHAR(30)  NOT NULL UNIQUE,
    floor               VARCHAR(20),
    owner_member_id     BIGINT REFERENCES members(id),
    occupancy           VARCHAR(20)  NOT NULL
                        CHECK (occupancy IN ('OWNER','TENANT','VACANT')),
    base_maintenance    NUMERIC(12,2) NOT NULL DEFAULT 0,
    tenant_surcharge    NUMERIC(12,2) NOT NULL DEFAULT 0,
    is_active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_units_unit_type ON units(unit_type);
CREATE INDEX idx_units_owner_member_id ON units(owner_member_id);
CREATE INDEX idx_units_is_active ON units(is_active);

-- -----------------------------------------------------------------------------
-- 3.3 unit_members
-- -----------------------------------------------------------------------------
CREATE TABLE unit_members (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    unit_id         BIGINT NOT NULL REFERENCES units(id) ON DELETE CASCADE,
    member_id       BIGINT NOT NULL REFERENCES members(id),
    relationship    VARCHAR(20) NOT NULL
                    CHECK (relationship IN ('OWNER','CO_OWNER','TENANT')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_unit_members_unit_member UNIQUE (unit_id, member_id)
);

CREATE INDEX idx_unit_members_unit_id ON unit_members(unit_id);
CREATE INDEX idx_unit_members_member_id ON unit_members(member_id);

-- -----------------------------------------------------------------------------
-- 3.4 maintenance_charges
-- -----------------------------------------------------------------------------
CREATE TABLE maintenance_charges (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    unit_id         BIGINT NOT NULL REFERENCES units(id),
    period_year     INT    NOT NULL,
    period_month    INT    NOT NULL CHECK (period_month BETWEEN 1 AND 12),
    amount_due      NUMERIC(12,2) NOT NULL,
    amount_paid     NUMERIC(12,2) NOT NULL DEFAULT 0,
    status          VARCHAR(20) NOT NULL DEFAULT 'DUE'
                    CHECK (status IN ('DUE','PARTIAL','PAID','VOID')),
    due_date        DATE NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_charge_unit_period UNIQUE (unit_id, period_year, period_month)
);

CREATE INDEX idx_charges_status ON maintenance_charges(status);
CREATE INDEX idx_charges_period ON maintenance_charges(period_year, period_month);
CREATE INDEX idx_charges_unit_id ON maintenance_charges(unit_id);

-- -----------------------------------------------------------------------------
-- 3.6 receipts (stub: payment_id column created WITHOUT FK; FK added later)
--      receipts must exist before payments (payments.receipt_id → receipts).
-- -----------------------------------------------------------------------------
CREATE TABLE receipts (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    receipt_number  VARCHAR(30) NOT NULL UNIQUE,
    payment_id      BIGINT NOT NULL UNIQUE,    -- FK added after payments table
    unit_id         BIGINT NOT NULL REFERENCES units(id),
    issued_to       BIGINT NOT NULL REFERENCES members(id),
    amount          NUMERIC(12,2) NOT NULL,
    issued_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    pdf_file_path   VARCHAR(500),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_receipts_unit_id ON receipts(unit_id);
CREATE INDEX idx_receipts_issued_to ON receipts(issued_to);

-- -----------------------------------------------------------------------------
-- 3.5 payments
-- -----------------------------------------------------------------------------
CREATE TABLE payments (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    unit_id             BIGINT NOT NULL REFERENCES units(id),
    submitted_by        BIGINT NOT NULL REFERENCES members(id),
    charge_id           BIGINT REFERENCES maintenance_charges(id),
    payment_type        VARCHAR(20) NOT NULL
                        CHECK (payment_type IN ('MAINTENANCE','TERRACE_BOOKING','OTHER')),
    amount              NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    method              VARCHAR(20) NOT NULL DEFAULT 'UPI'
                        CHECK (method IN ('UPI','BANK_TRANSFER','CASH')),
    utr_reference       VARCHAR(50),
    proof_file_path     VARCHAR(500),
    paid_at             TIMESTAMPTZ,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                        CHECK (status IN ('PENDING','VERIFIED','REJECTED')),
    verified_by         BIGINT REFERENCES members(id),
    verified_at         TIMESTAMPTZ,
    rejection_reason    VARCHAR(500),
    receipt_id          BIGINT REFERENCES receipts(id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_unit_id ON payments(unit_id);
CREATE INDEX idx_payments_submitted_by ON payments(submitted_by);
CREATE INDEX idx_payments_charge_id ON payments(charge_id);

-- Add the deferred FK from receipts.payment_id → payments.id now that payments exists.
ALTER TABLE receipts
    ADD CONSTRAINT fk_receipts_payment_id
    FOREIGN KEY (payment_id) REFERENCES payments(id);

-- -----------------------------------------------------------------------------
-- 3.7 society_expenses
-- -----------------------------------------------------------------------------
CREATE TABLE society_expenses (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    category        VARCHAR(30) NOT NULL
                    CHECK (category IN ('GARBAGE','ELECTRICITY','LIFT','WATER',
                                        'SECURITY','REPAIRS','ELECTRICAL_GOODS','OTHER')),
    title           VARCHAR(150) NOT NULL,
    vendor_name     VARCHAR(150),
    amount          NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    expense_date    DATE NOT NULL,
    paid_from       VARCHAR(50),
    bill_file_path  VARCHAR(500),
    note            VARCHAR(500),
    recorded_by     BIGINT NOT NULL REFERENCES members(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_expenses_category ON society_expenses(category);
CREATE INDEX idx_expenses_expense_date ON society_expenses(expense_date);

-- -----------------------------------------------------------------------------
-- 3.8 terrace_bookings
-- -----------------------------------------------------------------------------
CREATE TABLE terrace_bookings (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    unit_id         BIGINT REFERENCES units(id),
    booked_by       BIGINT NOT NULL REFERENCES members(id),
    event_title     VARCHAR(150) NOT NULL,
    event_date      DATE NOT NULL,
    nominal_fee     NUMERIC(12,2) NOT NULL DEFAULT 0,
    status          VARCHAR(20) NOT NULL DEFAULT 'REQUESTED'
                    CHECK (status IN ('REQUESTED','APPROVED','REJECTED','CANCELLED')),
    payment_id      BIGINT REFERENCES payments(id),
    approved_by     BIGINT REFERENCES members(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Only one APPROVED booking per date (partial unique index).
CREATE UNIQUE INDEX uq_bookings_one_approved_per_date
    ON terrace_bookings(event_date)
    WHERE status = 'APPROVED';

CREATE INDEX idx_bookings_status ON terrace_bookings(status);
CREATE INDEX idx_bookings_event_date ON terrace_bookings(event_date);

-- =============================================================================
-- Seed data
-- =============================================================================

-- Society defaults
INSERT INTO charge_config (default_flat_maintenance,
                           default_shop_maintenance,
                           default_tenant_surcharge,
                           default_due_day,
                           society_name)
VALUES (0, 0, 100, 10, 'My Society');

-- Seed ADMIN. password = 'admin123' (BCrypt strength 10).
-- Hash provided by spec; rotate immediately in production.
INSERT INTO members (full_name, phone, email, password_hash, role, is_active)
VALUES ('Admin',
        '9999999999',
        'admin@society.com',
        '$2b$10$jakjNx4YLzypIFKZq3S2seDr9abVa9J7rpRNVaGPXZMQVYBwt/kLC',
        'ADMIN',
        TRUE);
