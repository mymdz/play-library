# --- !Ups

ALTER TABLE books
ADD user_id bigint(20);

# --- !Downs

ALTER TABLE books
DROP COLUMN user_id;