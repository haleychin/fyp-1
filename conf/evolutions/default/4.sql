# --- !Ups
create table "students" (
  "id" BIGSERIAL NOT NULL PRIMARY KEY,
  "name" VARCHAR NOT NULL,
  "email" VARCHAR NOT NULL UNIQUE,
  "student_id" VARCHAR NOT NULL UNIQUE,
  "ic_or_passport" VARCHAR NOT NULL,"nationality" VARCHAR NOT NULL,
  "contact_number" VARCHAR NOT NULL,
  "birth_date" DATE NOT NULL,
  "programme" VARCHAR NOT NULL,
  "intake" VARCHAR NOT NULL,
  "semester" INTEGER NOT NULL,
  "created_at" timestamp default now() NOT NULL,
  "updated_at" timestamp default now() NOT NULL
)

# --- !Downs
DROP TABLE IF EXISTS "students"
