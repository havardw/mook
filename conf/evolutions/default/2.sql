# Tasks schema

# --- !Ups
create table user(name varchar(40) not null, email varchar(255) not null unique, hash varchar(255) not null);


# --- !Downs
DROP TABLE user;

