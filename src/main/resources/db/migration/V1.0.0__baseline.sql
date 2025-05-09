CREATE TABLE users (
                       id SERIAL PRIMARY KEY,
                       name varchar(40) NOT NULL,
                       email varchar(191) NOT NULL,
                       hash bytea,
                       UNIQUE (email)
);

CREATE TABLE sites (
                       id SERIAL PRIMARY KEY,
                       name varchar(80) NOT NULL,
                       slug varchar(40) NOT NULL,
                       UNIQUE (slug)
);

CREATE TABLE entry (
                       id SERIAL PRIMARY KEY,
                       entryDate date NOT NULL,
                       entryText text NOT NULL,
                       userId int NOT NULL REFERENCES users(id),
                       siteId int NOT NULL REFERENCES sites (id)
);

CREATE TABLE image (
                       id SERIAL PRIMARY KEY,
                       entryId int,
                       userId int NOT NULL REFERENCES users (id),
                       mimeType varchar(40) NOT NULL,
                       caption text,
                       siteId int NOT NULL REFERENCES sites (id)
);

CREATE TABLE userSession (
                             uuid UUID NOT NULL PRIMARY KEY,
                             userId int NOT NULL REFERENCES users (id),
                             expires timestamp NOT NULL
);


CREATE TYPE permission AS ENUM ('admin', 'edit');
CREATE TABLE permissions (
                             siteId int NOT NULL REFERENCES sites (id),
                             userId int NOT NULL REFERENCES users (id),
                             permission permission,
                             UNIQUE (siteId, userId)
);

