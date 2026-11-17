create table whatsapp_audio_media (
    id uuid primary key,
    inbound_message_id uuid not null,
    source_url varchar(1000) not null,
    mime_type varchar(100) null,
    storage_path varchar(500) not null,
    downloaded_at timestamp with time zone not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uq_whatsapp_audio_media_inbound_message_id unique (inbound_message_id),
    constraint fk_whatsapp_audio_media_inbound_message_id
        foreign key (inbound_message_id) references whatsapp_inbound_messages (id) on delete cascade
);

create index idx_whatsapp_audio_media_downloaded_at
    on whatsapp_audio_media (downloaded_at desc);
