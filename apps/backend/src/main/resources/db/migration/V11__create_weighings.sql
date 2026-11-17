create table weighings (
    id uuid primary key,
    farm_id uuid not null,
    animal_id uuid not null,
    peso_kg decimal(8, 2) not null,
    data_weighing date not null,
    tipo varchar(30) not null,
    origem varchar(30) not null,
    registrado_por uuid not null,
    idempotency_key varchar(100),
    audio_transcricao text,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_weighings_farm_id foreign key (farm_id) references farms (id),
    constraint fk_weighings_farm_id_animal_id foreign key (farm_id, animal_id) references animals (farm_id, id),
    constraint fk_weighings_farm_id_registrado_por foreign key (farm_id, registrado_por) references users (farm_id, id),
    constraint ck_weighings_peso_kg_positive check (peso_kg > 0),
    constraint ck_weighings_tipo check (tipo in ('rotina', 'entrada', 'saida', 'desmame', 'abate')),
    constraint ck_weighings_origem check (origem in ('app', 'whatsapp', 'iot')),
    constraint uq_weighings_farm_id_idempotency_key unique (farm_id, idempotency_key)
);

create index idx_weighings_farm_id_animal_id_data_weighing_desc
    on weighings (farm_id, animal_id, data_weighing desc);

create index idx_weighings_farm_id_data_weighing_desc
    on weighings (farm_id, data_weighing desc);

create index idx_weighings_farm_id_origem
    on weighings (farm_id, origem);
