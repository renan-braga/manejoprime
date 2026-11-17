create table vaccines (
    id UUID primary key,
    farm_id UUID not null,
    animal_id UUID null,
    lot_id UUID null,
    vaccine_type VARCHAR(200) not null,
    vaccine_batch VARCHAR(100) null,
    dose VARCHAR(30) not null,
    data_aplicacao DATE not null,
    data_proxima_dose DATE null,
    volume_ml DECIMAL(6,2) null,
    origem VARCHAR(30) not null,
    registrado_por UUID not null,
    created_at TIMESTAMP WITH TIME ZONE not null,
    updated_at TIMESTAMP WITH TIME ZONE not null,

    constraint fk_vaccines_farm
        foreign key (farm_id) references farms (id),
    constraint fk_vaccines_farm_animal
        foreign key (farm_id, animal_id) references animals (farm_id, id),
    constraint fk_vaccines_farm_lot
        foreign key (farm_id, lot_id) references lots (farm_id, id),
    constraint fk_vaccines_farm_registrado_por
        foreign key (farm_id, registrado_por) references users (farm_id, id),
    constraint chk_vaccines_target
        check ((animal_id is null) <> (lot_id is null)),
    constraint chk_vaccines_dose
        check (dose in ('primeira', 'segunda', 'reforco', 'anual')),
    constraint chk_vaccines_origem
        check (origem in ('app', 'whatsapp'))
);

create index idx_vaccines_farm_id_animal_id_data_aplicacao_desc
    on vaccines (farm_id, animal_id, data_aplicacao desc);

create index idx_vaccines_farm_id_lot_id_data_aplicacao_desc
    on vaccines (farm_id, lot_id, data_aplicacao desc);

create index idx_vaccines_farm_id_data_proxima_dose
    on vaccines (farm_id, data_proxima_dose);
