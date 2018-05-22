# --- !Ups
alter table "attendances" add constraint "fk_students" foreign key("student_id") references "students"("id") on update RESTRICT on delete CASCADE

# --- !Downs
DROP TABLE IF EXISTS "attendances"
