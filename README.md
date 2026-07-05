# Society Management Application

Residential + commercial society portal — units, members, monthly maintenance
charges, manual payment verification, receipts, expenses, and terrace bookings.

Authoritative spec: [`project.md`](./project.md).

This monorepo contains:

| Folder                       | What it is                                    |
|------------------------------|-----------------------------------------------|
| `society-mgmt-backend/`      | Java 21 + Spring Boot 3 + Postgres + Flyway   |
| `society-mgmt-frontend/`     | React 18 + Vite + MUI + React Query           |

> **Status: Phases 1–8 complete.** Bootstrap, Auth, Units/Members CRUD, Charge config,
> Payments + Receipt PDF, Admin dashboard, Society Expenses, and Terrace Bookings are all
> implemented. Phase 9 (hardening: tests, file validation, pagination tuning) is next.

---

## Prerequisites

- **Java 21** and **Maven 3.9+**
- **Node 18+** and **npm 9+**
- **PostgreSQL 16** running locally on port `5432`

Create the database once:

```bash
createdb society_mgmt
# or in psql:  CREATE DATABASE society_mgmt;
```

---

## Backend

```bash
cd society-mgmt-backend
cp .env.example .env       # optional — defaults work for local dev
mvn spring-boot:run
```

The app starts on `http://localhost:8080`. Flyway runs `V1__baseline.sql`
automatically on first launch, creating all 9 tables and seeding:

- `charge_config` (one row, "My Society", tenant surcharge ₹100, due day 10)
- `members` — one ADMIN: phone `9999999999`, password `admin123`
  (**rotate this password immediately**)

### Environment variables (override via env or `.env`)

| Variable           | Default                                          |
|--------------------|--------------------------------------------------|
| `DB_URL`           | `jdbc:postgresql://localhost:5432/society_mgmt`  |
| `DB_USER`          | `postgres`                                       |
| `DB_PASS`          | `postgres`                                       |
| `JWT_SECRET`       | dev placeholder — replace in prod                |
| `FRONTEND_ORIGIN`  | `http://localhost:5173`                          |
| `FILE_STORAGE_DIR` | `./uploads`                                      |
| `RECEIPT_PREFIX`   | `RCP`                                            |

Tests:

```bash
mvn test
```

---

## Frontend

```bash
cd society-mgmt-frontend
cp .env.example .env       # optional — proxy handles /api in dev
npm install
npm run dev
```

Vite dev server runs on `http://localhost:5173` and proxies `/api/*` to the
backend on `http://localhost:8080`.

Production build:

```bash
npm run build
```

The built assets land in `society-mgmt-frontend/dist/`. In a later phase these
are served from the Spring Boot `/static` resource path.

---

## Phases implemented

| Phase | What | Status |
|-------|------|--------|
| 1 | Bootstrap: Spring Boot scaffold, Flyway migration (all 9 tables), React + Vite + MUI | ✅ Done |
| 2 | Auth: JWT login/refresh/logout/me, member CRUD, seed ADMIN | ✅ Done |
| 3 | Units & Members CRUD, unit–member linking, frontend screens | ✅ Done |
| 4 | Charge config + monthly charge generation + member dues view | ✅ Done |
| 5 | Payments manual flow: submit proof → pending queue → verify/reject → PDF receipt | ✅ Done |
| 6 | Admin dashboard: summary, maintenance check, financials, outstanding | ✅ Done |
| 7 | Society expenses CRUD (admin) + read-only member view + category summary | ✅ Done |
| 8 | Terrace bookings: request (member) → approve/reject (admin), availability check | ✅ Done |
| 9 | Hardening: validation, integration tests, file MIME checks, pagination tuning | 🔜 Next |
