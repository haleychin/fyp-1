# --- !Ups
create unique index "question_unique" on "questions" ("exam_id","name")  
# --- !Downs
DROP TABLE IF EXISTS "questions"

