# --- !Ups
create table "attendances" (
  "course_id" BIGINT NOT NULL,
  "student_id" BIGINT NOT NULL,
  "group_id" INTEGER NOT NULL,
  "date" DATE NOT NULL,
  "attendance_type" VARCHAR NOT NULL,
  "created_at" timestamp default now() NOT NULL,
  "updated_at" timestamp default now() NOT NULL
)

# --- !Downs
DROP TABLE IF EXISTS "attendances"
