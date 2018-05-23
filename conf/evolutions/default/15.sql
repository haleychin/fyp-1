# --- !Ups
alter table "courseworks" add constraint "cw_fk_students" foreign key("student_id") references "students"("id") on update RESTRICT on delete CASCADE

# --- !Downs
DROP TABLE IF EXISTS "courseworks"

