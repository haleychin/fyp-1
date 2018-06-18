# --- !Ups
alter table "questions" add constraint "fk_exams" foreign key("exam_id") references "exams"("id") on update RESTRICT on delete CASCADE
# --- !Downs
DROP TABLE IF EXISTS "questions"

