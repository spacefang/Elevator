alter table alarms add column if not exists location varchar(255);
alter table alarms add column if not exists status varchar(32) not null default 'PENDING';

update alarms set level = upper(level) where level is not null and level != upper(level);

create index if not exists idx_alarms_status on alarms (status);

create table if not exists alarm_actions (
  id bigserial primary key,
  alarm_id bigint not null references alarms(id) on delete cascade,
  action_type varchar(32) not null,
  note text,
  operator_user_id varchar(64) not null,
  operator_name varchar(128) not null,
  operator_role varchar(64) not null,
  created_at timestamptz not null default now()
);

create index if not exists idx_alarm_actions_alarm_id on alarm_actions (alarm_id);
create index if not exists idx_alarm_actions_created_at on alarm_actions (created_at desc);
