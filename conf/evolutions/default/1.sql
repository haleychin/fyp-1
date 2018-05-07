# --- !Ups
create table "users" (
  "id" BIGSERIAL NOT NULL PRIMARY KEY,
  "name" VARCHAR NOT NULL,
  "email" VARCHAR NOT NULL UNIQUE,
  "password_hash" VARCHAR NOT NULL,
  "created_at" timestamp default now() NOT NULL,
  "updated_at" timestamp default now() NOT NULL
)

# --- !Downs
DROP TABLE IF EXISTS "users"
