# Add author to entry

# --- !Ups
alter table entry add column author varchar(255) not null default '';

# --- !Downs
alter table entry drop column author;

