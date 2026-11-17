alter table whatsapp_audio_media
    add column transcoded_mime_type varchar(100) null;

alter table whatsapp_audio_media
    add column transcoded_storage_path varchar(500) null;

alter table whatsapp_audio_media
    add column transcoded_at timestamp with time zone null;

create index idx_whatsapp_audio_media_transcoded_at
    on whatsapp_audio_media (transcoded_at desc);

alter table whatsapp_inbound_messages
    drop constraint chk_whatsapp_inbound_messages_status;

alter table whatsapp_inbound_messages
    add constraint chk_whatsapp_inbound_messages_status check (
        status in (
            'RECEIVED',
            'MEDIA_DOWNLOADED',
            'TRANSCODED',
            'TRANSCRIBED',
            'INTENT_EXTRACTED',
            'VALIDATED',
            'AWAITING_CONFIRMATION',
            'APPLIED',
            'REJECTED',
            'FAILED'
        )
    );

alter table whatsapp_processing_steps
    drop constraint chk_whatsapp_processing_steps_status;

alter table whatsapp_processing_steps
    add constraint chk_whatsapp_processing_steps_status check (
        status in (
            'RECEIVED',
            'MEDIA_DOWNLOADED',
            'TRANSCODED',
            'TRANSCRIBED',
            'INTENT_EXTRACTED',
            'VALIDATED',
            'AWAITING_CONFIRMATION',
            'APPLIED',
            'REJECTED',
            'FAILED'
        )
    );
