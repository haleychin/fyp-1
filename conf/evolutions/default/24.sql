# --- !Ups
alter table "questions_metrics" add constraint "qm_primary_key"
primary key("question_id","metric_id")
# --- !Downs
DROP TABLE IF EXISTS "questions_metrics"
