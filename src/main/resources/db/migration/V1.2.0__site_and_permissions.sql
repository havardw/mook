CREATE TABLE sites
(
    id   int AUTO_INCREMENT PRIMARY KEY,
    name varchar(80) NOT NULL,
    slug varchar(40) NOT NULL,
    UNIQUE (slug)
);

CREATE TABLE permissions
(
    siteId     int                    NOT NULL,
    userId     int                    NOT NULL,
    permission ENUM ('admin', 'edit') NOT NULL,
    CONSTRAINT FK_Permission_Site FOREIGN KEY (siteId) REFERENCES sites (id),
    CONSTRAINT FK_Permission_User FOREIGN KEY (userId) REFERENCES users (id),
    UNIQUE (siteId, userId)
);

-- Create a default site
INSERT INTO sites (name, slug)
VALUES ('Standard site', 'standard');

-- Add siteId column to entry table
ALTER TABLE entry
    ADD COLUMN siteId int NOT NULL DEFAULT 1;

ALTER TABLE entry
    ADD CONSTRAINT FK_Entry_Site FOREIGN KEY (siteId) REFERENCES sites (id);

-- Add siteId column to image table
ALTER TABLE image
    ADD COLUMN siteId int NOT NULL DEFAULT 1;

ALTER TABLE image
    ADD CONSTRAINT FK_Image_Site FOREIGN KEY (siteId) REFERENCES sites (id);

-- Populate permissions from active users
INSERT INTO permissions (siteId, userId, permission)
SELECT 1, id, 'edit'
FROM users
WHERE active = true;
