create table alimentacoes (
    id uuid primary key,
    farm_id uuid not null,
    animal_id uuid null,
    lot_id uuid null,
    tipo_alimento varchar(30) not null,
    descricao text null,
    quantidade_kg decimal(10,2) not null,
    custo_por_kg decimal(10,2) null,
    data_inicio date not null,
    data_fim date null,
    origem varchar(30) not null,
    registrado_por uuid not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,

    constraint fk_alimentacoes_farm
        foreign key (farm_id) references farms (id),
    constraint fk_alimentacoes_farm_animal
        foreign key (farm_id, animal_id) references animals (farm_id, id),
    constraint fk_alimentacoes_farm_lot
        foreign key (farm_id, lot_id) references lots (farm_id, id),
    constraint fk_alimentacoes_farm_registrado_por
        foreign key (farm_id, registrado_por) references users (farm_id, id),
    constraint ck_alimentacoes_target_xor
        check ((animal_id is not null and lot_id is null) or (animal_id is null and lot_id is not null)),
    constraint ck_alimentacoes_tipo_alimento
        check (tipo_alimento in ('pasto', 'racao', 'silagem', 'suplemento', 'sal_mineral', 'misto')),
    constraint ck_alimentacoes_origem
        check (origem in ('app', 'whatsapp')),
    constraint ck_alimentacoes_data_interval
        check (data_fim is null or data_fim >= data_inicio)
);

create index idx_alimentacoes_farm_id_animal_id_data_inicio_desc
    on alimentacoes (farm_id, animal_id, data_inicio desc);

create index idx_alimentacoes_farm_id_lot_id_data_inicio_desc
    on alimentacoes (farm_id, lot_id, data_inicio desc);

create index idx_alimentacoes_farm_id_data_inicio_desc
    on alimentacoes (farm_id, data_inicio desc);
