# --- !Ups
alter table "courseworks" add constraint "pk_coursework" primary key("course_id","student_id","name")

# --- !Downs
DROP TABLE IF EXISTS "courseworks"
