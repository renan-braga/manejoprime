alter table occurrences
    add constraint uk_occurrences_farm_id_id unique (farm_id, id);

create table medications (
    id uuid primary key,
    farm_id uuid not null,
    animal_id uuid not null,
    occurrence_id uuid null,
    medication_name varchar(200) not null,
    principio_ativo varchar(200) null,
    motivo text null,
    dosagem varchar(100) not null,
    data_aplicacao date not null,
    periodo_withdrawal integer not null,
    release_date date not null,
    custo decimal(10,2) null,
    origem varchar(30) not null,
    registrado_por uuid not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,

    constraint fk_medications_farm
        foreign key (farm_id) references farms (id),
    constraint fk_medications_farm_animal
        foreign key (farm_id, animal_id) references animals (farm_id, id),
    constraint fk_medications_farm_occurrence
        foreign key (farm_id, occurrence_id) references occurrences (farm_id, id),
    constraint fk_medications_farm_registrado_por
        foreign key (farm_id, registrado_por) references users (farm_id, id),
    constraint ck_medications_origem
        check (origem in ('app', 'whatsapp')),
    constraint ck_medications_periodo_withdrawal
        check (periodo_withdrawal >= 0)
);

create index idx_medications_farm_id_animal_id_data_aplicacao_desc
    on medications (farm_id, animal_id, data_aplicacao desc);

create index idx_medications_farm_id_release_date
    on medications (farm_id, release_date);

create index idx_medications_farm_id_occurrence_id
    on medications (farm_id, occurrence_id);
