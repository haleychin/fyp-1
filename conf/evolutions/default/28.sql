# --- !Ups
create table "filters" (
  "id" BIGSERIAL NOT NULL PRIMARY KEY,
  "course_id" BIGINT NOT NULL,
  "attendance_rate" INTEGER NOT NULL,
  "attendance_rate_point" DOUBLE PRECISION NOT NULL,
  "consecutive_missed" INTEGER NOT NULL,
  "consecutive_missed_point" DOUBLE PRECISION NOT NULL,
  "absent_count" INTEGER NOT NULL,
  "absent_count_point" DOUBLE PRECISION NOT NULL,
  "passing_mark" INTEGER NOT NULL,
  "overview_threshold" DOUBLE PRECISION NOT NULL,
  "attendance_threshold" DOUBLE PRECISION NOT NULL,
  "coursework_threshold" DOUBLE PRECISION NOT NULL,
  "coursework_mark" INTEGER NOT NULL,
  "coursework_mark_point" DOUBLE PRECISION NOT NULL)

# --- !Downs
DROP TABLE IF EXISTS "filters"
