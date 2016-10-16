-- Add IDs to user table, MySQL will fill in the IDs
ALTER TABLE user ADD COLUMN id INT NOT NULL AUTO_INCREMENT PRIMARY KEY;

-- Add user ID as foreign key
ALTER TABLE entry ADD COLUMN userId INT;

-- Do a bunch of these as manual updates, or write a loop in a procedure
-- update entry set userId=1 where author='Håvard';


-- Make userId mandatory after it is populated
ALTER TABLE entry MODIFY COLUMN userId INT NOT NULL;

-- Remove old column when everything's OK
ALTER TABLE entry DROP COLUMN author;
