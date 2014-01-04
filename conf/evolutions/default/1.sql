# Tasks schema

# --- !Ups
create table entry(id integer NOT NULL AUTO_INCREMENT, entryDate date NOT NULL, entryText text NOT NULL, PRIMARY KEY(id));


# --- !Downs

DROP TABLE entry;
