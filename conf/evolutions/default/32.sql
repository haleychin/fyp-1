# --- !Ups
create unique index "metric_unique" on "metrics" ("course_id","name")
# --- !Downs
DROP TABLE IF EXISTS "metrics"
