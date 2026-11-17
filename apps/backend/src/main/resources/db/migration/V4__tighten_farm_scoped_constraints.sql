alter table lots
    add constraint uq_lots_farm_id_id unique (farm_id, id);

alter table users
    add constraint uq_users_farm_id_id unique (farm_id, id);

alter table animals
    add constraint uq_animals_farm_id_id unique (farm_id, id);

alter table auth_sessions
    add constraint uq_auth_sessions_id_farm_id_user_id unique (id, farm_id, user_id);

alter table animals
    add constraint fk_animals_farm_id_lot_id
        foreign key (farm_id, lot_id) references lots (farm_id, id);

alter table animals
    add constraint fk_animals_farm_id_dam_id
        foreign key (farm_id, dam_id) references animals (farm_id, id);

alter table animals
    add constraint fk_animals_farm_id_sire_id
        foreign key (farm_id, sire_id) references animals (farm_id, id);

alter table magic_link_tokens
    add constraint fk_magic_link_tokens_farm_id_user_id
        foreign key (farm_id, user_id) references users (farm_id, id);

alter table auth_sessions
    add constraint fk_auth_sessions_farm_id_user_id
        foreign key (farm_id, user_id) references users (farm_id, id);

alter table refresh_tokens
    add constraint fk_refresh_tokens_farm_id_user_id
        foreign key (farm_id, user_id) references users (farm_id, id);

alter table refresh_tokens
    add constraint fk_refresh_tokens_session_id_farm_id_user_id
        foreign key (session_id, farm_id, user_id) references auth_sessions (id, farm_id, user_id);

create index idx_refresh_tokens_farm_id on refresh_tokens (farm_id);
