# --- !Ups

CREATE TABLE "people" (
  "id" bigserial primary key,
  "name" character varying,
  "age" integer,
  "created_at" timestamp NOT NULL default now(),
  "updated_at" timestamp NOT NULL default now()
)

# --- !Downs

DROP TABLE IF EXISTS "people"
