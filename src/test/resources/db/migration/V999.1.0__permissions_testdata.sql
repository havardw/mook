-- Add permissions for test users
-- This ensures that test users have access to the standard site
-- which is required for authentication after removing the active flag

-- Get user IDs for test users
SET @unni_id = (SELECT id FROM users WHERE email = 'unni@example.org');
SET @test_id = (SELECT id FROM users WHERE email = 'test@example.org');

-- Add edit permissions for the standard site (id=1)
INSERT INTO permissions (siteId, userId, permission) VALUES (1, @unni_id, 'edit');
INSERT INTO permissions (siteId, userId, permission) VALUES (1, @test_id, 'edit');