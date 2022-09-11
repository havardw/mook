
CREATE TABLE users (
    id int IDENTITY PRIMARY KEY,
    name nvarchar(40) NOT NULL,
    email nvarchar(191) NOT NULL,
    hash binary(64),
    UNIQUE (email)
);

CREATE TABLE entry (
    id int IDENTITY PRIMARY KEY,
    entryDate date NOT NULL,
    entryText ntext NOT NULL,
    userId int NOT NULL,
    CONSTRAINT FK_Entry_User FOREIGN KEY (userId) REFERENCES users (id)
);

CREATE TABLE image (
    id int IDENTITY PRIMARY KEY,
    entryId int,
    userId int NOT NULL,
    mimeType nvarchar(40) NOT NULL,
    caption ntext,
    CONSTRAINT FK_Image_User FOREIGN KEY (userId) REFERENCES users (id)
);

CREATE TABLE userSession (
    uuid varchar(40) NOT NULL PRIMARY KEY,
    userId int NOT NULL,
    expires datetime NOT NULL,
    CONSTRAINT FK_Session_User FOREIGN KEY (userId) REFERENCES users (id)
);
