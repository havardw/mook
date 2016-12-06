CREATE TABLE entry (
  id int(11) NOT NULL AUTO_INCREMENT,
  entryDate date NOT NULL,
  entryText text NOT NULL,
  userId int NOT NULL,
  PRIMARY KEY (id)
);

-- Max length for index on varchar is 191 in MariaDB when using utf8mb4
CREATE TABLE user (
  id int NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name varchar(40) NOT NULL,
  email varchar(191) NOT NULL,
  hash varchar(191) NOT NULL,
  UNIQUE (email)
);

CREATE TABLE image (
  id int NOT NULL AUTO_INCREMENT PRIMARY KEY,
  entryId int,
  userId int NOT NULL,
  mimeType varchar(40) NOT NULL,
  caption text
);