# --- !Ups
alter table "students" add column "fail_count" INTEGER DEFAULT 0;
# --- !Downs
alter table "students" drop column "fail_count";
