-- Remove the active flag from users table as it's no longer needed
-- Users are now considered active if they have permissions for any sites
ALTER TABLE users DROP COLUMN active;