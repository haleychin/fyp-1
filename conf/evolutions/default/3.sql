# --- !Ups
alter table "courses" add constraint "user_fk" foreign key("user_id")
references "users"("id") on update RESTRICT on delete CASCADE

# --- !Downs
DROP TABLE IF EXISTS "courses"
