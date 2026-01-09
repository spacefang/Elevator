create table if not exists alarms (
  id bigserial primary key,
  device_id varchar(64) not null,
  level varchar(16) not null,
  type varchar(64) not null,
  description text,
  occurred_at timestamptz not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists idx_alarms_device_id on alarms (device_id);
create index if not exists idx_alarms_occurred_at on alarms (occurred_at desc);

