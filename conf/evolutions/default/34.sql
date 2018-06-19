# --- !Ups
alter table "courses" add column "completed" BOOLEAN default false;
# --- !Downs
alter table "courses" drop column "completed";
