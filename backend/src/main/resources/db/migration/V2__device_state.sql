create table if not exists device_states (
  id bigserial primary key,
  device_id varchar(64) not null unique,
  online boolean not null default false,
  last_seen_at timestamptz not null,
  floor int,
  direction varchar(16),
  door_status varchar(16),
  speed double precision,
  load_value double precision,
  temperature double precision,
  vibration double precision,
  power double precision,
  updated_at timestamptz not null default now()
);

create index if not exists idx_device_states_last_seen_at on device_states (last_seen_at);

create table if not exists device_state_changes (
  id bigserial primary key,
  device_id varchar(64) not null,
  event_type varchar(32) not null,
  details text,
  occurred_at timestamptz not null default now()
);

create index if not exists idx_device_state_changes_device_id on device_state_changes (device_id);
create index if not exists idx_device_state_changes_occurred_at on device_state_changes (occurred_at desc);

