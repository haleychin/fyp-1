# --- !Ups
alter table "attendances" add constraint "pk_attendance" primary key("course_id","student_id","group_id","date")

# --- !Downs
DROP TABLE IF EXISTS "attendances"
