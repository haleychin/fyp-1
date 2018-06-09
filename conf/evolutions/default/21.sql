# --- !Ups
create table "questions" (
  "id" BIGSERIAL NOT NULL PRIMARY KEY,
  "student_id" BIGINT NOT NULL,
  "course_id" BIGINT NOT NULL,
  "name" VARCHAR NOT NULL,
  "mark" DOUBLE PRECISION NOT NULL,
  "totalMark" DOUBLE PRECISION NOT NULL,
  "created_at" timestamp default now() NOT NULL,
  "updated_at" timestamp default now() NOT NULL
)

# --- !Downs
DROP TABLE IF EXISTS "questions"
