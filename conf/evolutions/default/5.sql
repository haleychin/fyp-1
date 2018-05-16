# --- !Ups
create table "courses_students" (
  "course_id" BIGINT NOT NULL,
  "student_id" BIGINT NOT NULL,
  "created_at" timestamp default now() NOT NULL,
  "updated_at" timestamp default now() NOT NULL
)

# --- !Downs
DROP TABLE IF EXISTS "courses_students"
