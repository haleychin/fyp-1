# --- !Ups
alter table "exams" add constraint "e_fk_courses" foreign key("course_id") references "courses"("id") on update RESTRICT on delete CASCADE

# --- !Downs
DROP TABLE IF EXISTS "exams"
