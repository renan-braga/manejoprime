alter table whatsapp_inbound_messages
    add column correlation_id varchar(255) null;

alter table whatsapp_inbound_messages
    add column failure_reason text null;

create index idx_whatsapp_inbound_messages_correlation_id
    on whatsapp_inbound_messages (correlation_id);

create table whatsapp_extracted_payloads (
    id uuid primary key,
    inbound_message_id uuid not null,
    provider varchar(50) not null,
    provider_model varchar(100) not null,
    intent varchar(100) null,
    structured_payload text not null,
    extracted_at timestamp with time zone not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uq_whatsapp_extracted_payloads_inbound_message_id unique (inbound_message_id),
    constraint fk_whatsapp_extracted_payloads_inbound_message_id
        foreign key (inbound_message_id) references whatsapp_inbound_messages (id) on delete cascade
);

create index idx_whatsapp_extracted_payloads_extracted_at
    on whatsapp_extracted_payloads (extracted_at desc);

create table whatsapp_outbound_messages (
    id uuid primary key,
    inbound_message_id uuid not null,
    farm_id uuid null,
    user_id uuid null,
    correlation_id varchar(255) not null,
    phone_number varchar(32) not null,
    message_type varchar(100) not null,
    payload text not null,
    provider_message_id varchar(255) null,
    status varchar(50) not null,
    failure_reason text null,
    queued_at timestamp with time zone not null,
    sent_at timestamp with time zone null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uq_whatsapp_outbound_messages_provider_message_id unique (provider_message_id),
    constraint fk_whatsapp_outbound_messages_inbound_message_id
        foreign key (inbound_message_id) references whatsapp_inbound_messages (id) on delete cascade,
    constraint fk_whatsapp_outbound_messages_farm_id
        foreign key (farm_id) references farms (id),
    constraint fk_whatsapp_outbound_messages_farm_id_user_id
        foreign key (farm_id, user_id) references users (farm_id, id),
    constraint chk_whatsapp_outbound_messages_status check (
        status in ('QUEUED', 'SENT', 'FAILED')
    )
);

create index idx_whatsapp_outbound_messages_inbound_message_id_created_at
    on whatsapp_outbound_messages (inbound_message_id, created_at desc);

create index idx_whatsapp_outbound_messages_farm_id_created_at
    on whatsapp_outbound_messages (farm_id, created_at desc);

create index idx_whatsapp_outbound_messages_status
    on whatsapp_outbound_messages (status);

create index idx_whatsapp_outbound_messages_phone_number
    on whatsapp_outbound_messages (phone_number);
