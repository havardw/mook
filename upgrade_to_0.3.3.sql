CREATE TABLE userSession (
  uuid varchar(40) NOT NULL PRIMARY KEY,
  userId int NOT NULL,
  expires timestamp NOT NULL
);