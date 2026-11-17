create table billing_customer_data (
    farm_id       uuid primary key references farms(id),
    customer_type text        not null default 'company',
    name          text,
    document      text,
    email         text,
    phone         text,
    postal_code   text,
    address       text,
    address_number text,
    city          text,
    state         text,
    updated_at    timestamp with time zone not null
);
