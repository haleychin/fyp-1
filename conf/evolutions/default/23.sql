# --- !Ups
create table "questions_metrics" (
  "question_id" BIGINT NOT NULL,
  "metric_id" BIGINT NOT NULL,
  "created_at" timestamp default now() NOT NULL,
  "updated_at" timestamp default now() NOT NULL
)
# --- !Downs
DROP TABLE IF EXISTS "questions_metrics"

