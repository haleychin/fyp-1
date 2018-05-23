# --- !Ups
create table "courseworks" (
  "course_id" BIGINT NOT NULL,
  "student_id" BIGINT NOT NULL,
  "name" VARCHAR NOT NULL,
  "mark" DOUBLE PRECISION NOT NULL,
  "created_at" timestamp default now() NOT NULL,
  "updated_at" timestamp default now() NOT NULL
)

# --- !Downs
DROP TABLE IF EXISTS "courseworks"
