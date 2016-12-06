CREATE TABLE image (
  id int NOT NULL AUTO_INCREMENT PRIMARY KEY,
  entryId int,
  userId int NOT NULL,
  mimeType varchar(40) NOT NULL,
  caption text
);