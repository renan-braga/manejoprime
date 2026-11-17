create table offers (
    id uuid primary key,
    farm_id uuid not null,
    title varchar(200) not null,
    notes text,
    asking_price decimal(12, 2) not null,
    buyer_contact varchar(200),
    expires_at date,
    status varchar(30) not null default 'draft',
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_offers_farm_id foreign key (farm_id) references farms (id),
    constraint ck_offers_status check (status in ('draft', 'active', 'sold', 'cancelled')),
    constraint ck_offers_asking_price_positive check (asking_price > 0)
);

create index idx_offers_farm_id on offers (farm_id);
create index idx_offers_farm_id_status on offers (farm_id, status);

create table offer_animals (
    offer_id uuid not null,
    animal_id uuid not null,
    primary key (offer_id, animal_id),
    constraint fk_offer_animals_offer_id foreign key (offer_id) references offers (id),
    constraint fk_offer_animals_animal_id foreign key (animal_id) references animals (id)
);

create index idx_offer_animals_offer_id on offer_animals (offer_id);
