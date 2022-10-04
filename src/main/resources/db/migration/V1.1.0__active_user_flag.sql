-- Add "active" flag to users table
ALTER TABLE users ADD COLUMN active BOOLEAN DEFAULT true;
