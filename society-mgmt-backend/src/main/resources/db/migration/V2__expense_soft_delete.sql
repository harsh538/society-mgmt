-- Soft-delete support for society_expenses (project.md § 1.2 rule 4: financial rows never hard-deleted)
ALTER TABLE society_expenses ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;
