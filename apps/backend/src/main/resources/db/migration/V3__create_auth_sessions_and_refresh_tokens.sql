create table auth_sessions (
    id uuid primary key,
    farm_id uuid not null,
    user_id uuid not null,
    revoked_at timestamp with time zone,
    revocation_reason varchar(64),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_auth_sessions_farm_id foreign key (farm_id) references farms (id),
    constraint fk_auth_sessions_user_id foreign key (user_id) references users (id),
    constraint ck_auth_sessions_revocation_reason check (
        revocation_reason is null or revocation_reason in ('logged_out', 'replaced_by_new_login')
    ),
    constraint ck_auth_sessions_revocation_fields check (
        (revoked_at is null and revocation_reason is null)
        or (revoked_at is not null and revocation_reason is not null)
    )
);

create table refresh_tokens (
    id uuid primary key,
    session_id uuid not null,
    farm_id uuid not null,
    user_id uuid not null,
    expires_at timestamp with time zone not null,
    consumed_at timestamp with time zone,
    revoked_at timestamp with time zone,
    revocation_reason varchar(64),
    replaced_by_token_id uuid,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_refresh_tokens_session_id foreign key (session_id) references auth_sessions (id),
    constraint fk_refresh_tokens_farm_id foreign key (farm_id) references farms (id),
    constraint fk_refresh_tokens_user_id foreign key (user_id) references users (id),
    constraint ck_refresh_tokens_revocation_reason check (
        revocation_reason is null or revocation_reason in ('logged_out', 'replaced_by_new_login')
    ),
    constraint ck_refresh_tokens_revocation_fields check (
        (revoked_at is null and revocation_reason is null)
        or (revoked_at is not null and revocation_reason is not null)
    ),
    constraint ck_refresh_tokens_replacement_fields check (
        replaced_by_token_id is null or consumed_at is not null
    )
);

create index idx_auth_sessions_user_id on auth_sessions (user_id);
create index idx_auth_sessions_farm_id on auth_sessions (farm_id);
create index idx_refresh_tokens_session_id on refresh_tokens (session_id);
create index idx_refresh_tokens_user_id on refresh_tokens (user_id);
create index idx_refresh_tokens_expires_at on refresh_tokens (expires_at);
