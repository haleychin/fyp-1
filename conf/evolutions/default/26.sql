# --- !Ups
alter table "questions_metrics" add constraint "fk_questions"
foreign key("question_id") references "questions"("id") on update RESTRICT on delete CASCADE

# --- !Downs
DROP TABLE IF EXISTS "questions_metrics"
