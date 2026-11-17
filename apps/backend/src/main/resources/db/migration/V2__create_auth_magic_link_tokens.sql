create table magic_link_tokens (
    id uuid primary key,
    farm_id uuid not null,
    user_id uuid not null,
    token varchar(128) not null,
    expires_at timestamp with time zone not null,
    consumed_at timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uq_magic_link_tokens_token unique (token),
    constraint fk_magic_link_tokens_farm_id foreign key (farm_id) references farms (id),
    constraint fk_magic_link_tokens_user_id foreign key (user_id) references users (id)
);

create index idx_magic_link_tokens_farm_id on magic_link_tokens (farm_id);
create index idx_magic_link_tokens_user_id on magic_link_tokens (user_id);
create index idx_magic_link_tokens_expires_at on magic_link_tokens (expires_at);
