# --- !Ups
alter table "questions_metrics" add constraint "fk_metrics"
foreign key("metric_id") references "metrics"("id") on update RESTRICT on delete CASCADE
# --- !Downs
DROP TABLE IF EXISTS "questions_metrics"
