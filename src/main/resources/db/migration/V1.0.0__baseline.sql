
CREATE TABLE users (
    id int AUTO_INCREMENT PRIMARY KEY,
    name varchar(40) NOT NULL,
    email varchar(191) NOT NULL,
    hash binary(64),
    UNIQUE (email)
);

CREATE TABLE entry (
    id int AUTO_INCREMENT PRIMARY KEY,
    entryDate date NOT NULL,
    entryText text NOT NULL,
    userId int NOT NULL,
    CONSTRAINT FK_Entry_User FOREIGN KEY (userId) REFERENCES users (id)
);

CREATE TABLE image (
    id int AUTO_INCREMENT PRIMARY KEY,
    entryId int,
    userId int NOT NULL,
    mimeType varchar(40) NOT NULL,
    caption text,
    CONSTRAINT FK_Image_User FOREIGN KEY (userId) REFERENCES users (id)
);

CREATE TABLE userSession (
    uuid varchar(40) NOT NULL PRIMARY KEY,
    userId int NOT NULL,
    expires datetime NOT NULL,
    CONSTRAINT FK_Session_User FOREIGN KEY (userId) REFERENCES users (id)
);
