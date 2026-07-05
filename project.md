# Society Management Application — Project Specification

> **Purpose of this document:** This is the single source of truth for building the
> Society Management Application. It is written so that a developer (human or AI) can
> read this file end-to-end and implement the full application without further
> clarification. Every table, field, endpoint, role, and business rule is defined here.

---

## 1. Overview

A web application to manage a residential + commercial society. It tracks units
(flats, shops, terrace), members (owners/tenants), monthly maintenance charges,
manual payment verification (UTR/screenshot + admin approval), receipts, society
expenses, and terrace event bookings. It exposes an admin dashboard with unit counts,
per-unit maintenance status, and society-wide outstanding balance.

### 1.1 Tech Stack

| Layer        | Technology                                              |
|--------------|---------------------------------------------------------|
| Frontend     | React 18 + Vite, React Router, TanStack Query, MUI      |
| Backend      | Java 21 + Spring Boot 3.x (Web, Data JPA, Security)     |
| Database     | PostgreSQL 16                                           |
| Auth         | JWT (access + refresh), BCrypt password hashing         |
| Migrations   | Flyway                                                  |
| PDF receipts | OpenPDF                                                 |
| File storage | Local disk (dev) / S3-compatible bucket (prod)          |
| Build/Deploy | Maven (backend), Vite build served by Spring `/static` |

### 1.2 Key Design Principles

1. **Money is `BigDecimal` (Java) / `NUMERIC(12,2)` (Postgres)** — never floating point.
2. **Outstanding balance is always derived** from the ledger (charges − verified
   payments), never stored as a mutable column.
3. **Payment verification is manual**: member uploads UTR + screenshot → payment row
   created with status `PENDING` → admin approves/rejects → on approval a receipt is
   generated and the charge is reconciled.
4. **Soft deletes** via `is_active` / `deleted_at` where history matters
   (members, units). Financial rows (charges, payments, receipts, expenses) are
   **never hard-deleted**; they are reversed/voided.
5. **Role-based access**: `ADMIN`, `MEMBER`. All money-mutating endpoints require
   `ADMIN` except payment submission (member submits own payment).
6. **All timestamps** stored in UTC (`TIMESTAMPTZ`), rendered in IST on the frontend.

---

## 2. Roles & Permissions

| Role     | Capabilities                                                                 |
|----------|------------------------------------------------------------------------------|
| `ADMIN`  | Full access: manage units, members, charge config, generate charges, verify/reject payments, issue receipts, log expenses, manage terrace bookings, view dashboard. |
| `MEMBER` | View own profile, own units, own dues & payment history, submit a payment for verification, request a terrace booking, view society expense summary (read-only). |

A `MEMBER` may be linked to one or more units. The **owner** of a unit is always the
financially responsible party; a tenant is informational (their presence triggers the
configurable tenant surcharge on that unit's charge). Receipts are issued to the
owner member of the unit by default.

---

## 3. Database Schema (PostgreSQL)

All tables use `BIGINT GENERATED ALWAYS AS IDENTITY` primary keys unless noted.
`created_at` / `updated_at` are `TIMESTAMPTZ NOT NULL DEFAULT now()` on every table
(omitted from field lists below for brevity; include them everywhere).

### 3.1 `members`
Stores every person with a login or a financial relationship to the society.

| Field            | Type            | Constraints / Notes                                  |
|------------------|-----------------|------------------------------------------------------|
| id               | BIGINT PK       |                                                      |
| full_name        | VARCHAR(150)    | NOT NULL                                             |
| phone            | VARCHAR(15)     | NOT NULL, UNIQUE — primary login identifier          |
| email            | VARCHAR(150)    | UNIQUE, nullable                                     |
| password_hash    | VARCHAR(255)    | nullable (set when account activated)                |
| role             | VARCHAR(20)     | NOT NULL, CHECK IN ('ADMIN','MEMBER'), default MEMBER|
| is_active        | BOOLEAN         | NOT NULL default true                                |
| last_login_at    | TIMESTAMPTZ     | nullable                                             |

### 3.2 `units`
A flat, shop, or the terrace.

| Field             | Type           | Constraints / Notes                                  |
|-------------------|----------------|------------------------------------------------------|
| id                | BIGINT PK      |                                                      |
| unit_type         | VARCHAR(20)    | NOT NULL, CHECK IN ('FLAT','SHOP','TERRACE')         |
| unit_number       | VARCHAR(30)    | NOT NULL — e.g. "A-101", "Shop-7". UNIQUE per society|
| floor             | VARCHAR(20)    | nullable                                             |
| owner_member_id   | BIGINT FK      | → members(id), nullable for TERRACE                  |
| occupancy         | VARCHAR(20)    | NOT NULL, CHECK IN ('OWNER','TENANT','VACANT')       |
| base_maintenance  | NUMERIC(12,2)  | NOT NULL default 0 — monthly base for this unit      |
| tenant_surcharge  | NUMERIC(12,2)  | NOT NULL default 0 — extra added when occupancy=TENANT (the configurable "+100") |
| is_active         | BOOLEAN        | NOT NULL default true                                |

> **Charge amount rule:** `amount_due = base_maintenance + (occupancy = 'TENANT' ? tenant_surcharge : 0)`.
> TERRACE units are not charged monthly maintenance; they generate `terrace_bookings` with a nominal fee instead.

### 3.3 `unit_members`
Links members to units (supports co-owners / tenant occupants). Many-to-many.

| Field        | Type        | Constraints / Notes                                       |
|--------------|-------------|-----------------------------------------------------------|
| id           | BIGINT PK   |                                                           |
| unit_id      | BIGINT FK   | → units(id), NOT NULL                                     |
| member_id    | BIGINT FK   | → members(id), NOT NULL                                   |
| relationship | VARCHAR(20) | NOT NULL, CHECK IN ('OWNER','CO_OWNER','TENANT')          |
|              |             | UNIQUE(unit_id, member_id)                                |

### 3.4 `maintenance_charges`
One row per unit per billing period. Generated monthly.

| Field        | Type           | Constraints / Notes                                       |
|--------------|----------------|-----------------------------------------------------------|
| id           | BIGINT PK      |                                                           |
| unit_id      | BIGINT FK      | → units(id), NOT NULL                                     |
| period_year  | INT            | NOT NULL                                                  |
| period_month | INT            | NOT NULL, CHECK 1..12                                     |
| amount_due   | NUMERIC(12,2)  | NOT NULL — snapshot of computed amount at generation time |
| amount_paid  | NUMERIC(12,2)  | NOT NULL default 0 — sum of VERIFIED payments allocated   |
| status       | VARCHAR(20)    | NOT NULL, CHECK IN ('DUE','PARTIAL','PAID','VOID'), default DUE |
| due_date     | DATE           | NOT NULL                                                  |
|              |                | UNIQUE(unit_id, period_year, period_month)               |

> `amount_paid` is a convenience rollup updated inside the payment-verification
> transaction. Status transitions: DUE → PARTIAL → PAID as payments are verified.

### 3.5 `payments`
A member-submitted payment awaiting or having completed manual verification.

| Field             | Type           | Constraints / Notes                                              |
|-------------------|----------------|------------------------------------------------------------------|
| id                | BIGINT PK      |                                                                  |
| unit_id           | BIGINT FK      | → units(id), NOT NULL                                            |
| submitted_by      | BIGINT FK      | → members(id), NOT NULL — who submitted                          |
| charge_id         | BIGINT FK      | → maintenance_charges(id), nullable (may be advance/booking)     |
| payment_type      | VARCHAR(20)    | NOT NULL, CHECK IN ('MAINTENANCE','TERRACE_BOOKING','OTHER')     |
| amount            | NUMERIC(12,2)  | NOT NULL, CHECK > 0                                              |
| method            | VARCHAR(20)    | NOT NULL, CHECK IN ('UPI','BANK_TRANSFER','CASH'), default UPI    |
| utr_reference     | VARCHAR(50)    | nullable — UPI/bank reference number entered by member           |
| proof_file_path   | VARCHAR(500)   | nullable — uploaded screenshot path                              |
| paid_at           | TIMESTAMPTZ    | nullable — when member says they paid                            |
| status            | VARCHAR(20)    | NOT NULL, CHECK IN ('PENDING','VERIFIED','REJECTED'), default PENDING |
| verified_by       | BIGINT FK      | → members(id), nullable — admin who acted                        |
| verified_at       | TIMESTAMPTZ    | nullable                                                         |
| rejection_reason  | VARCHAR(500)   | nullable                                                         |
| receipt_id        | BIGINT FK      | → receipts(id), nullable — set on verification                   |

### 3.6 `receipts`
Generated automatically when a payment is verified.

| Field          | Type           | Constraints / Notes                                  |
|----------------|----------------|------------------------------------------------------|
| id             | BIGINT PK      |                                                      |
| receipt_number | VARCHAR(30)    | NOT NULL, UNIQUE — e.g. "RCP-2026-000123"            |
| payment_id     | BIGINT FK      | → payments(id), NOT NULL, UNIQUE                     |
| unit_id        | BIGINT FK      | → units(id), NOT NULL                                |
| issued_to      | BIGINT FK      | → members(id), NOT NULL                              |
| amount         | NUMERIC(12,2)  | NOT NULL                                             |
| issued_at      | TIMESTAMPTZ    | NOT NULL default now()                               |
| pdf_file_path  | VARCHAR(500)   | nullable — generated PDF location                    |

### 3.7 `society_expenses`
Money paid out by the society (garbage, lightbill, lift, electrical, etc.).

| Field         | Type           | Constraints / Notes                                                  |
|---------------|----------------|----------------------------------------------------------------------|
| id            | BIGINT PK      |                                                                      |
| category      | VARCHAR(30)    | NOT NULL, CHECK IN ('GARBAGE','ELECTRICITY','LIFT','WATER','SECURITY','REPAIRS','ELECTRICAL_GOODS','OTHER') |
| title         | VARCHAR(150)   | NOT NULL — short description e.g. "June lift AMC"                     |
| vendor_name   | VARCHAR(150)   | nullable                                                             |
| amount        | NUMERIC(12,2)  | NOT NULL, CHECK > 0                                                  |
| expense_date  | DATE           | NOT NULL                                                             |
| paid_from     | VARCHAR(50)    | nullable — e.g. "Society Current A/C"                                |
| bill_file_path| VARCHAR(500)   | nullable — uploaded bill/invoice                                     |
| note          | VARCHAR(500)   | nullable                                                             |
| recorded_by   | BIGINT FK      | → members(id), NOT NULL                                              |

### 3.8 `terrace_bookings`
Event bookings on the terrace for a nominal fee.

| Field         | Type           | Constraints / Notes                                          |
|---------------|----------------|--------------------------------------------------------------|
| id            | BIGINT PK      |                                                              |
| unit_id       | BIGINT FK      | → units(id), nullable — booking member's home unit           |
| booked_by     | BIGINT FK      | → members(id), NOT NULL                                      |
| event_title   | VARCHAR(150)   | NOT NULL                                                     |
| event_date    | DATE           | NOT NULL                                                     |
| nominal_fee   | NUMERIC(12,2)  | NOT NULL default 0                                           |
| status        | VARCHAR(20)    | NOT NULL, CHECK IN ('REQUESTED','APPROVED','REJECTED','CANCELLED'), default REQUESTED |
| payment_id    | BIGINT FK      | → payments(id), nullable                                    |
| approved_by   | BIGINT FK      | → members(id), nullable                                     |
|               |                | partial unique index on (event_date) WHERE status='APPROVED' (one event/day) |

### 3.9 `charge_config`
Society-wide defaults used when generating charges / creating units. Single-row table
(or keyed config) so admin can change defaults without code changes.

| Field                     | Type          | Constraints / Notes                          |
|---------------------------|---------------|----------------------------------------------|
| id                        | BIGINT PK     |                                              |
| default_flat_maintenance  | NUMERIC(12,2) | NOT NULL default 0                           |
| default_shop_maintenance  | NUMERIC(12,2) | NOT NULL default 0                           |
| default_tenant_surcharge  | NUMERIC(12,2) | NOT NULL default 100                         |
| default_due_day           | INT           | NOT NULL default 10 — day of month due       |
| society_name              | VARCHAR(150)  | NOT NULL                                      |
| society_upi_id            | VARCHAR(100)  | nullable — shown on the QR/payment screen     |
| society_bank_details      | VARCHAR(500)  | nullable                                      |

### 3.10 Derived: Outstanding Balance
Not a table. Computed via query:

```sql
-- Per unit
SELECT u.id, u.unit_number,
       COALESCE(SUM(mc.amount_due - mc.amount_paid), 0) AS outstanding
FROM units u
LEFT JOIN maintenance_charges mc
       ON mc.unit_id = u.id AND mc.status <> 'VOID'
WHERE u.is_active
GROUP BY u.id, u.unit_number;

-- Society-wide outstanding = SUM(amount_due - amount_paid) over non-void charges
-- Society net position = total verified income - total expenses
```

### 3.11 Entity Relationship Summary
```
members 1───* unit_members *───1 units
units   1───* maintenance_charges
units   1───* payments *───0..1 maintenance_charges
payments 1───1 receipts
units   1───* terrace_bookings
members 1───* society_expenses (recorded_by)
charge_config (singleton)
```

---

## 4. Backend Architecture (Spring Boot)

### 4.1 Package Structure
```
com.society.app
├── config/            # SecurityConfig, CorsConfig, JwtConfig, OpenApiConfig
├── security/          # JwtAuthFilter, JwtService, CustomUserDetailsService
├── member/            # Member entity, repo, service, controller, dto
├── unit/              # Unit + UnitMember entity, repo, service, controller, dto
├── charge/            # MaintenanceCharge + ChargeConfig, charge-generation service
├── payment/           # Payment entity, repo, service, controller, dto
├── receipt/           # Receipt entity, repo, service, PdfReceiptGenerator
├── expense/           # SocietyExpense entity, repo, service, controller, dto
├── booking/           # TerraceBooking entity, repo, service, controller, dto
├── dashboard/         # DashboardService + controller (aggregations)
├── storage/           # FileStorageService (local/S3 abstraction)
├── common/            # BaseEntity, ApiResponse, exceptions, GlobalExceptionHandler
└── SocietyApplication.java
```

### 4.2 Cross-cutting Conventions
- **Response envelope:** all endpoints return `ApiResponse<T> { boolean success; T data; String message; }`.
- **Errors:** `GlobalExceptionHandler` maps exceptions to HTTP codes with a consistent
  error body `{ success:false, message, errorCode, fieldErrors? }`.
- **Validation:** Jakarta Bean Validation (`@Valid`) on all request DTOs.
- **Pagination:** list endpoints accept `?page=0&size=20&sort=field,dir`; return
  `{ content, page, size, totalElements, totalPages }`.
- **Auth:** `Authorization: Bearer <accessToken>`. Access token 15 min, refresh 7 days.
- **Money serialization:** `BigDecimal` as string with 2 decimals.
- **Transactions:** payment verification, charge generation, and booking approval are
  `@Transactional` with row-level updates to keep `amount_paid` / status consistent.

---

## 5. REST API Specification

Base path: `/api/v1`. All endpoints require auth except `/auth/login` and `/auth/refresh`.
**[A]** = ADMIN only, **[M]** = MEMBER (own data), **[A/M]** = both (member scoped to self).

### 5.1 Auth
| Method | Path                  | Access | Body / Notes                                              |
|--------|-----------------------|--------|-----------------------------------------------------------|
| POST   | /auth/login           | public | `{ phone, password }` → `{ accessToken, refreshToken, member }` |
| POST   | /auth/refresh         | public | `{ refreshToken }` → new tokens                           |
| POST   | /auth/logout          | A/M    | invalidate refresh token                                  |
| GET    | /auth/me              | A/M    | current member profile                                    |
| POST   | /auth/change-password | A/M    | `{ oldPassword, newPassword }`                            |

### 5.2 Members  [A] for all except `/me` views
| Method | Path                     | Access | Notes                                            |
|--------|--------------------------|--------|--------------------------------------------------|
| GET    | /members                 | A      | paginated, filter `?role=&search=`               |
| POST   | /members                 | A      | create member `{ fullName, phone, email, role }` |
| GET    | /members/{id}            | A      | detail incl. linked units                        |
| PUT    | /members/{id}            | A      | update                                           |
| DELETE | /members/{id}            | A      | soft delete (is_active=false)                    |
| POST   | /members/{id}/activate   | A      | set password / send activation                  |

### 5.3 Units
| Method | Path                  | Access | Notes                                                       |
|--------|-----------------------|--------|-------------------------------------------------------------|
| GET    | /units                | A      | paginated, filter `?type=FLAT|SHOP|TERRACE&occupancy=`      |
| POST   | /units                | A      | create `{ unitType, unitNumber, floor, ownerMemberId, occupancy, baseMaintenance, tenantSurcharge }` |
| GET    | /units/{id}           | A/M    | member can read only own units                              |
| PUT    | /units/{id}           | A      | update incl. maintenance fields                             |
| DELETE | /units/{id}           | A      | soft delete                                                 |
| POST   | /units/{id}/members   | A      | link member `{ memberId, relationship }`                    |
| DELETE | /units/{id}/members/{memberId} | A | unlink                                               |
| GET    | /units/{id}/charges   | A/M    | charge history for the unit                                 |
| GET    | /units/{id}/outstanding | A/M  | derived outstanding for the unit                            |

### 5.4 Charge Config & Generation  [A]
| Method | Path                       | Access | Notes                                                  |
|--------|----------------------------|--------|--------------------------------------------------------|
| GET    | /charge-config             | A      | current society defaults                               |
| PUT    | /charge-config             | A      | update defaults                                        |
| POST   | /charges/generate          | A      | `{ year, month }` → creates charges for all active FLAT/SHOP units (idempotent: skips existing period). Returns count created. |
| GET    | /charges                   | A      | paginated, filter `?year=&month=&status=&unitId=`      |
| GET    | /charges/{id}              | A      | detail                                                 |
| POST   | /charges/{id}/void         | A      | void a charge with reason                              |

### 5.5 Payments (Manual Verification Flow)
| Method | Path                      | Access | Notes                                                                 |
|--------|---------------------------|--------|-----------------------------------------------------------------------|
| GET    | /payments                 | A      | paginated, filter `?status=PENDING|VERIFIED|REJECTED&unitId=`         |
| GET    | /payments/pending         | A      | the admin verification queue (status=PENDING, oldest first)           |
| POST   | /payments                 | M      | submit payment (multipart: json part + proof file). Body `{ unitId, chargeId?, paymentType, amount, method, utrReference, paidAt }`. Creates row with status=PENDING. |
| GET    | /payments/{id}            | A/M    | detail; member only own                                               |
| GET    | /payments/mine            | M      | current member's payment history                                      |
| POST   | /payments/{id}/verify     | A      | **approve**: validate amount, allocate to charge, set VERIFIED, generate receipt, update charge `amount_paid`+status. All in one transaction. |
| POST   | /payments/{id}/reject     | A      | `{ rejectionReason }` → status=REJECTED                               |
| GET    | /payments/{id}/proof      | A/M    | stream the uploaded proof image                                       |

#### Verification transaction (server logic, authoritative)
```
1. Load payment (must be PENDING) FOR UPDATE.
2. If payment_type = MAINTENANCE and charge_id present:
      load charge FOR UPDATE.
      new_paid = charge.amount_paid + payment.amount
      charge.amount_paid = new_paid
      charge.status = new_paid >= charge.amount_due ? 'PAID'
                    : new_paid > 0 ? 'PARTIAL' : 'DUE'
3. Generate receipt_number = "RCP-{year}-{zeroPaddedSeq}".
4. Insert receipt row; render PDF; store path.
5. payment.status=VERIFIED, verified_by, verified_at, receipt_id set.
6. Commit. (Any failure → rollback, payment stays PENDING.)
```

### 5.6 Receipts
| Method | Path                    | Access | Notes                                  |
|--------|-------------------------|--------|----------------------------------------|
| GET    | /receipts               | A      | paginated list                         |
| GET    | /receipts/{id}          | A/M    | detail; member only own                |
| GET    | /receipts/{id}/pdf      | A/M    | download/stream the receipt PDF        |

### 5.7 Society Expenses
| Method | Path                | Access | Notes                                                        |
|--------|---------------------|--------|--------------------------------------------------------------|
| GET    | /expenses           | A/M    | paginated, filter `?category=&from=&to=`; members read-only  |
| POST   | /expenses           | A      | multipart: json + optional bill file                         |
| GET    | /expenses/{id}      | A/M    | detail                                                        |
| PUT    | /expenses/{id}      | A      | update                                                        |
| DELETE | /expenses/{id}      | A      | delete (or void)                                             |
| GET    | /expenses/summary   | A/M    | `?from=&to=` → totals grouped by category                    |

### 5.8 Terrace Bookings
| Method | Path                        | Access | Notes                                            |
|--------|-----------------------------|--------|--------------------------------------------------|
| GET    | /bookings                   | A      | all bookings, filter `?status=&from=&to=`        |
| GET    | /bookings/mine              | M      | current member's bookings                        |
| POST   | /bookings                   | M      | request `{ eventTitle, eventDate, unitId? }`     |
| POST   | /bookings/{id}/approve      | A      | set APPROVED + nominal_fee; checks date free     |
| POST   | /bookings/{id}/reject       | A      | `{ reason }`                                     |
| POST   | /bookings/{id}/cancel       | A/M    | member can cancel own REQUESTED                  |
| GET    | /bookings/availability      | A/M    | `?month=&year=` → list of taken dates            |

### 5.9 Dashboard  [A]
| Method | Path                       | Access | Returns                                                                |
|--------|----------------------------|--------|------------------------------------------------------------------------|
| GET    | /dashboard/summary         | A      | `{ flatCount, shopCount, terraceCount, totalUnits, ownerOccupied, tenantOccupied, vacant }` |
| GET    | /dashboard/maintenance     | A      | per-unit rows `{ unitNumber, type, occupancy, ownerName, amountDue, amountPaid, outstanding, status }` + `?status=&type=` filters |
| GET    | /dashboard/financials      | A      | `{ totalBilled, totalCollected, totalOutstanding, totalExpenses, netPosition, pendingPaymentsCount }` |
| GET    | /dashboard/outstanding     | A      | society-wide + per-unit outstanding list                               |

---

## 6. Frontend Architecture (React)

### 6.1 Folder Structure
```
src/
├── api/            # axios instance, endpoint modules (auth.js, units.js, ...)
├── auth/           # AuthContext, useAuth, ProtectedRoute, RoleRoute
├── components/     # shared: DataTable, MoneyText, StatusChip, FileUpload, PageHeader
├── layouts/        # AdminLayout (sidebar), MemberLayout (simpler nav)
├── pages/
│   ├── auth/       # Login, ChangePassword
│   ├── admin/      # Dashboard, Units, Members, ChargeConfig, GenerateCharges,
│   │               # PaymentQueue, Payments, Receipts, Expenses, Bookings
│   └── member/     # MyProfile, MyUnits, MyDues, SubmitPayment, MyPayments,
│   │               # MyReceipts, ExpensesView, MyBookings
├── hooks/          # useUnits, usePayments, etc. (TanStack Query wrappers)
├── theme/          # MUI theme (responsive breakpoints)
└── App.jsx         # router + providers
```

### 6.2 Responsive Rules
- Mobile-first. Admin sidebar collapses to a drawer below `md` (900px).
- Tables (`DataTable`) switch to stacked cards below `sm` (600px).
- All forms single-column on mobile, two-column on desktop.
- Min tap target 44px; payment "submit proof" uses native file/camera input.

### 6.3 Key Screens → API mapping
| Screen                 | Primary endpoints                                              |
|------------------------|---------------------------------------------------------------|
| Admin Dashboard        | /dashboard/summary, /dashboard/financials                     |
| Maintenance Check page | /dashboard/maintenance (+ filters)                            |
| Payment Queue          | /payments/pending → verify/reject                             |
| Units management       | /units CRUD + /units/{id}/members                             |
| Expenses               | /expenses CRUD + /expenses/summary                            |
| Member Dues            | /units/{id}/outstanding, /units/{id}/charges                  |
| Submit Payment         | /charge-config (show UPI/QR), POST /payments (multipart)      |
| My Receipts            | /receipts/mine list, /receipts/{id}/pdf                       |

---

## 7. Business Rules (authoritative)

1. **Charge amount** = `base_maintenance + (occupancy='TENANT' ? tenant_surcharge : 0)`.
   Snapshotted into `maintenance_charges.amount_due` at generation; later config
   changes do not retro-edit existing charges.
2. **Charge generation is idempotent** per (unit, year, month). Re-running skips
   existing rows. Only active FLAT and SHOP units are billed. TERRACE excluded.
3. **Due date** = `default_due_day` of the billing month from `charge_config`.
4. **A payment can only be verified once.** Verification is the only path that creates
   a receipt and mutates `amount_paid`.
5. **Rejected payments** never touch charges or receipts; member may resubmit.
6. **Outstanding** is always `SUM(amount_due - amount_paid)` over non-VOID charges.
7. **Receipt numbers** are sequential per calendar year, zero-padded, never reused.
8. **One approved terrace booking per date.**
9. **Members see only their own** payments, receipts, dues, bookings; expenses are
   read-only and society-wide for transparency.
10. **Net position** (dashboard) = total VERIFIED income − total expenses.

---

## 8. Security Requirements
- Passwords hashed with BCrypt (strength ≥ 10).
- JWT signed with HS256, secret from env var `JWT_SECRET`; access 15 min, refresh 7 days.
- Endpoint authorization enforced via `@PreAuthorize("hasRole('ADMIN')")` etc.
- Members cannot access other members' resources — service layer checks ownership.
- File uploads: validate MIME (jpeg/png/pdf), max 5 MB, randomized stored filename,
  served only through authorized endpoints (no public bucket listing).
- CORS restricted to the frontend origin via env `FRONTEND_ORIGIN`.
- All money-mutating endpoints idempotency-safe / transactional.

---

## 9. Environment & Configuration
| Variable            | Purpose                                  |
|---------------------|------------------------------------------|
| DB_URL/USER/PASS    | Postgres connection                      |
| JWT_SECRET          | token signing                            |
| FRONTEND_ORIGIN     | CORS allowlist                           |
| FILE_STORAGE_DIR    | local upload dir (dev)                    |
| S3_* (prod)         | bucket/region/keys                       |
| RECEIPT_PREFIX      | e.g. "RCP"                               |

---

## 10. Build Order (implementation roadmap)
1. **Bootstrap:** Spring Boot project, Postgres, Flyway baseline migration with all
   tables in section 3. React + Vite + MUI scaffold, axios + auth context.
2. **Auth:** members table, login/refresh/me, JWT, ProtectedRoute, seed one ADMIN.
3. **Units & Members CRUD** + linking + frontend management screens.
4. **Charge config + generation** + member dues view.
5. **Payments manual flow:** submit (multipart) → pending queue → verify/reject →
   receipt PDF generation. Core feature.
6. **Admin dashboard:** summary, maintenance check, financials, outstanding.
7. **Expenses** CRUD + summary + read-only member view.
8. **Terrace bookings** request/approve flow.
9. **Hardening:** validation, error handling, file checks, pagination, tests.

---

## 11. Acceptance Checklist
- [ ] Admin can create flats, shops, and terrace units with configurable maintenance + tenant surcharge.
- [ ] Tenant-occupied units automatically include the surcharge in generated charges.
- [ ] Monthly charge generation is idempotent and skips terrace.
- [ ] Member can view own dues and full maintenance history.
- [ ] Member can submit a UPI/bank payment with UTR + screenshot.
- [ ] Admin sees a pending-verification queue and can approve or reject.
- [ ] Approval generates a sequential receipt PDF and reconciles the charge.
- [ ] Member can download their receipts.
- [ ] Admin dashboard shows flat/shop counts and a maintenance-status page per unit.
- [ ] Society-wide outstanding balance is shown and correct.
- [ ] Admin can log society expenses by category with bills; members can view them.
- [ ] Terrace bookings can be requested and approved with a nominal fee, one per date.
- [ ] App is fully usable on a phone browser and a laptop browser.
