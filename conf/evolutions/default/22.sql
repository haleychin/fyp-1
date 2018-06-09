# --- !Ups
create table "metrics" (
  "id" BIGSERIAL NOT NULL PRIMARY KEY,
  "course_id" BIGINT NOT NULL,
  "name" VARCHAR NOT NULL,
  "description" VARCHAR NOT NULL,
  "created_at" timestamp default now() NOT NULL,
  "updated_at" timestamp default now() NOT NULL
)

# --- !Downs
DROP TABLE IF EXISTS "metrics"
