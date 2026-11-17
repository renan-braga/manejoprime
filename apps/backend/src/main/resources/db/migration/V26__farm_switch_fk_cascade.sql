-- Farm-switch updates users.farm_id; composite FKs on auth_sessions and refresh_tokens
-- must cascade so existing sessions stay consistent while they are being revoked.
alter table auth_sessions
    drop constraint fk_auth_sessions_farm_id_user_id;
alter table auth_sessions
    add constraint fk_auth_sessions_farm_id_user_id
        foreign key (farm_id, user_id) references users (farm_id, id)
        on update cascade on delete cascade;

alter table refresh_tokens
    drop constraint fk_refresh_tokens_farm_id_user_id;
alter table refresh_tokens
    add constraint fk_refresh_tokens_farm_id_user_id
        foreign key (farm_id, user_id) references users (farm_id, id)
        on update cascade on delete cascade;

alter table refresh_tokens
    drop constraint fk_refresh_tokens_session_id_farm_id_user_id;
alter table refresh_tokens
    add constraint fk_refresh_tokens_session_id_farm_id_user_id
        foreign key (session_id, farm_id, user_id) references auth_sessions (id, farm_id, user_id)
        on update cascade on delete cascade;
