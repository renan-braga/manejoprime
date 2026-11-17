create table occurrences (
    id uuid primary key,
    farm_id uuid not null,
    animal_id uuid not null,
    tipo varchar(30) not null,
    gravidade varchar(20) not null,
    descricao text not null,
    foto_url varchar(500),
    origem varchar(30) not null,
    registrado_por uuid not null,
    status varchar(30) not null,
    resolvido_por uuid,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_occurrences_farm_id foreign key (farm_id) references farms (id),
    constraint fk_occurrences_farm_id_animal_id foreign key (farm_id, animal_id) references animals (farm_id, id),
    constraint fk_occurrences_farm_id_registrado_por foreign key (farm_id, registrado_por) references users (farm_id, id),
    constraint fk_occurrences_farm_id_resolvido_por foreign key (farm_id, resolvido_por) references users (farm_id, id),
    constraint ck_occurrences_tipo check (tipo in ('health', 'behavior', 'injury', 'parasite', 'reproduction', 'nutrition', 'other')),
    constraint ck_occurrences_gravidade check (gravidade in ('low', 'medium', 'high', 'critical')),
    constraint ck_occurrences_origem check (origem in ('app', 'whatsapp')),
    constraint ck_occurrences_status check (status in ('open', 'in_follow_up', 'resolved')),
    constraint ck_occurrences_resolved_consistency check (
        (status = 'resolved' and resolvido_por is not null)
        or (status <> 'resolved' and resolvido_por is null)
    )
);

create index idx_occurrences_farm_id_animal_id_created_at_desc
    on occurrences (farm_id, animal_id, created_at desc);

create index idx_occurrences_farm_id_status_gravidade
    on occurrences (farm_id, status, gravidade);

create index idx_occurrences_farm_id_created_at_desc
    on occurrences (farm_id, created_at desc);
