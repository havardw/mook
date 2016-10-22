CREATE TABLE entry (
  id int(11) NOT NULL AUTO_INCREMENT,
  entryDate date NOT NULL,
  entryText text NOT NULL,
  userId int NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE user (
  id int NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name varchar(40) NOT NULL,
  email varchar(255) NOT NULL,
  hash varchar(255) NOT NULL,
  UNIQUE KEY (email)
);

CREATE TABLE image (
  id int NOT NULL AUTO_INCREMENT PRIMARY KEY,
  userId int NOT NULL,
  mimeType varchar(40) NOT NULL
)