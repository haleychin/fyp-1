# --- !Ups
alter table "metrics" add constraint "fk_courses"
foreign key("course_id") references "courses"("id") on update RESTRICT on delete CASCADE

# --- !Downs
DROP TABLE IF EXISTS "metrics"
