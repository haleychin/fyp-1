# --- !Ups
create table "courses" (
  "id" BIGSERIAL NOT NULL PRIMARY KEY,
  "user_id" BIGINT NOT NULL,
  "title" VARCHAR NOT NULL,
  "created_at" timestamp default now() NOT NULL,
  "updated_at" timestamp default now() NOT NULL
)

# --- !Downs
DROP TABLE IF EXISTS "courses"
