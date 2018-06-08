# --- !Ups
create table "metrics" (
  "id" BIGSERIAL NOT NULL PRIMARY KEY,
  "name" VARCHAR NOT NULL,
  "description" VARCHAR NOT NULL,
  "created_at" timestamp default now() NOT NULL,
  "updated_at" timestamp default now() NOT NULL
)

# --- !Downs
DROP TABLE IF EXISTS "metrics"
