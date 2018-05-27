# --- !Ups
alter table "exams" add constraint "pk_exam" primary key("course_id","student_id")

# --- !Downs
DROP TABLE IF EXISTS "exams"
