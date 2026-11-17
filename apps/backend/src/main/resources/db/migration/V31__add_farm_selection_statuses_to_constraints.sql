-- V30 introduced PENDING_FARM_SELECTION and FARM_SELECTED to the domain but
-- forgot to extend the check constraints on both whatsapp tables.
-- Inserting these statuses fails with a constraint violation until this migration runs.

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
            'FAILED',
            'PENDING_FARM_SELECTION',
            'FARM_SELECTED'
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
            'FAILED',
            'PENDING_FARM_SELECTION',
            'FARM_SELECTED'
        )
    );
