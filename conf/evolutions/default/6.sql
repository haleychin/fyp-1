# --- !Ups
alter table "courses_students" add constraint "primary_key" primary key("course_id","student_id")

# --- !Downs
DROP TABLE IF EXISTS "courses_students"

