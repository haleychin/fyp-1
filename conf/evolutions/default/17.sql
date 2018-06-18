# --- !Ups
create unique index "exam_unique" on "exams" ("course_id","student_id")
# --- !Downs
DROP TABLE IF EXISTS "exams"
