# --- !Ups
create table "exams" (
  "id" BIGSERIAL NOT NULL PRIMARY KEY,
  "course_id" BIGINT NOT NULL,
  "student_id" BIGINT NOT NULL,
  "mark" DOUBLE PRECISION NOT NULL,
  "total_mark" DOUBLE PRECISION NOT NULL,
  "weightage" DOUBLE PRECISION NOT NULL,
  "total_weightage" DOUBLE PRECISION NOT NULL,
  "created_at" timestamp default now() NOT NULL,
  "updated_at" timestamp default now() NOT NULL
)

# --- !Downs
DROP TABLE IF EXISTS "exams"
