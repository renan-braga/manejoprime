create table invitations (
    id uuid primary key,
    farm_id uuid not null,
    email varchar(255) not null,
    phone varchar(20),
    role varchar(30) not null,
    status varchar(20) not null default 'pending',
    invited_by uuid,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_invitations_farm_id foreign key (farm_id) references farms (id),
    constraint fk_invitations_invited_by foreign key (invited_by) references users (id),
    constraint ck_invitations_role check (role in ('admin', 'veterinarian', 'worker')),
    constraint ck_invitations_status check (status in ('pending', 'accepted', 'revoked'))
);

create index idx_invitations_farm_id on invitations (farm_id);
create index idx_invitations_email on invitations (email);
create index idx_invitations_farm_id_status on invitations (farm_id, status);
