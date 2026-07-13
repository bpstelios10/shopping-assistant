create table if not exists user_memory (
    user_id    text        primary key,
    profile    jsonb       not null default '{}',
    updated_at timestamptz not null default now()
);
