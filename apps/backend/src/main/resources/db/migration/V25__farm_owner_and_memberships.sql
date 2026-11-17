alter table farms
    add column owner_user_id uuid references users(id);

create table user_farm_memberships (
    user_id   uuid                     not null references users(id),
    farm_id   uuid                     not null references farms(id),
    role      varchar(50)              not null default 'admin',
    joined_at timestamp with time zone not null default now(),
    primary key (user_id, farm_id)
);

create index user_farm_memberships_farm_id_idx on user_farm_memberships(farm_id);
