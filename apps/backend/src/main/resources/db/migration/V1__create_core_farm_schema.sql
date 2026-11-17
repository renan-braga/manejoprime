create table farms (
    id uuid primary key,
    nome varchar(255) not null,
    localizacao text,
    area_hectares decimal(12, 2),
    rendimento_carcaca decimal(5, 2) not null default 50.00,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint ck_farms_rendimento_carcaca_range check (rendimento_carcaca >= 0 and rendimento_carcaca <= 100)
);

create table lots (
    id uuid primary key,
    farm_id uuid not null,
    nome varchar(100) not null,
    finalidade varchar(30) not null,
    pasto_area varchar(100),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_lots_farm_id foreign key (farm_id) references farms (id),
    constraint ck_lots_finalidade check (finalidade in ('breeding', 'raising', 'fattening', 'reproduction'))
);

create index idx_lots_farm_id on lots (farm_id);
create index idx_lots_farm_id_finalidade on lots (farm_id, finalidade);

create table users (
    id uuid primary key,
    farm_id uuid not null,
    telefone varchar(20) not null,
    nome varchar(255) not null,
    email varchar(255),
    role varchar(30) not null,
    ativo boolean not null default true,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uq_users_telefone unique (telefone),
    constraint fk_users_farm_id foreign key (farm_id) references farms (id),
    constraint ck_users_role check (role in ('admin', 'veterinarian', 'worker'))
);

create index idx_users_farm_id on users (farm_id);
create index idx_users_farm_id_role on users (farm_id, role);
create index idx_users_farm_id_ativo on users (farm_id, ativo);

create table animals (
    id uuid primary key,
    farm_id uuid not null,
    lot_id uuid,
    identificacao varchar(50) not null,
    nome varchar(100),
    raca varchar(100),
    sexo varchar(20) not null,
    data_nascimento date,
    peso_nascimento decimal(8, 2),
    dam_id uuid,
    sire_id uuid,
    status varchar(30) not null,
    data_entrada date,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uq_animals_farm_id_identificacao unique (farm_id, identificacao),
    constraint fk_animals_farm_id foreign key (farm_id) references farms (id),
    constraint fk_animals_lot_id foreign key (lot_id) references lots (id),
    constraint fk_animals_dam_id foreign key (dam_id) references animals (id),
    constraint fk_animals_sire_id foreign key (sire_id) references animals (id),
    constraint ck_animals_sexo check (sexo in ('macho', 'femea')),
    constraint ck_animals_status check (status in ('ativo', 'vendido', 'morto', 'transferido'))
);

create index idx_animals_farm_id on animals (farm_id);
create index idx_animals_farm_id_lot_id on animals (farm_id, lot_id);
create index idx_animals_farm_id_status on animals (farm_id, status);
create index idx_animals_farm_id_sexo on animals (farm_id, sexo);
