create table whatsapp_transcripts (
    id uuid primary key,
    inbound_message_id uuid not null,
    provider varchar(50) not null,
    provider_model varchar(100) not null,
    transcript text not null,
    transcribed_at timestamp with time zone not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uq_whatsapp_transcripts_inbound_message_id unique (inbound_message_id),
    constraint fk_whatsapp_transcripts_inbound_message_id
        foreign key (inbound_message_id) references whatsapp_inbound_messages (id) on delete cascade
);

create index idx_whatsapp_transcripts_transcribed_at
    on whatsapp_transcripts (transcribed_at desc);
