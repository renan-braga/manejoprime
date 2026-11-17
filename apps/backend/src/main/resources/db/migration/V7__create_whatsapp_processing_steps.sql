create table whatsapp_processing_steps (
    id uuid primary key,
    inbound_message_id uuid not null,
    status varchar(50) not null,
    details text null,
    occurred_at timestamp with time zone not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_whatsapp_processing_steps_inbound_message_id
        foreign key (inbound_message_id) references whatsapp_inbound_messages (id) on delete cascade,
    constraint chk_whatsapp_processing_steps_status check (
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

create index idx_whatsapp_processing_steps_inbound_message_id_created_at
    on whatsapp_processing_steps (inbound_message_id, created_at desc);
create index idx_whatsapp_processing_steps_status on whatsapp_processing_steps (status);
