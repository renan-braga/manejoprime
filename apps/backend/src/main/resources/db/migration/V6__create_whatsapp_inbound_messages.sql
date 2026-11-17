create table whatsapp_inbound_messages (
    id uuid primary key,
    provider_message_id varchar(255) not null,
    event_type varchar(100) not null,
    phone_number varchar(32) not null,
    farm_id uuid null,
    user_id uuid null,
    message_type varchar(100) null,
    raw_payload text not null,
    status varchar(50) not null,
    received_at timestamp with time zone not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uq_whatsapp_inbound_messages_provider_message_id unique (provider_message_id),
    constraint fk_whatsapp_inbound_messages_farm_id foreign key (farm_id) references farms (id),
    constraint fk_whatsapp_inbound_messages_farm_id_user_id foreign key (farm_id, user_id) references users (farm_id, id),
    constraint chk_whatsapp_inbound_messages_status check (
        status in (
            'RECEIVED',
            'MEDIA_DOWNLOADED',
            'TRANSCRIBED',
            'INTENT_EXTRACTED',
            'VALIDATED',
            'AWAITING_CONFIRMATION',
            'APPLIED',
            'REJECTED',
            'FAILED'
        )
    )
);

create index idx_whatsapp_inbound_messages_farm_id_created_at on whatsapp_inbound_messages (farm_id, created_at desc);
create index idx_whatsapp_inbound_messages_status on whatsapp_inbound_messages (status);
create index idx_whatsapp_inbound_messages_phone_number on whatsapp_inbound_messages (phone_number);
