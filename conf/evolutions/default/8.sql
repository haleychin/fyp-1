# --- !Ups
alter table "courses_students" add constraint "fk_students" foreign key("student_id") references "students"("id") on update RESTRICT on delete CASCADE

# --- !Downs
DROP TABLE IF EXISTS "courses_students"

