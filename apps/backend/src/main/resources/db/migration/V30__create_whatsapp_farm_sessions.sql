create table whatsapp_farm_sessions (
    phone_number  varchar(30)  not null,
    farm_id       uuid         not null references farms(id),
    farm_name     varchar(255) not null,
    selected_date date         not null,
    created_at    timestamp with time zone not null default now(),
    primary key (phone_number, selected_date)
);
