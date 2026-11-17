create table media_references (
    id uuid primary key,
    owner_type varchar(40) not null,
    owner_id uuid not null,
    purpose varchar(60) not null,
    external_url varchar(1000),
    storage_path varchar(500),
    mime_type varchar(100),
    file_size_bytes bigint,
    checksum_sha256 varchar(64),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uq_media_references_owner_purpose unique (owner_type, owner_id, purpose),
    constraint ck_media_references_owner_type check (owner_type in ('occurrence', 'whatsapp_audio_media')),
    constraint ck_media_references_purpose check (
        purpose in (
            'occurrence_photo',
            'whatsapp_source_audio',
            'whatsapp_downloaded_audio',
            'whatsapp_transcoded_audio'
        )
    ),
    constraint ck_media_references_single_location check (
        (external_url is not null and storage_path is null)
        or (external_url is null and storage_path is not null)
    ),
    constraint ck_media_references_external_url_scheme check (
        external_url is null
        or external_url like 'http://%'
        or external_url like 'https://%'
    ),
    constraint ck_media_references_file_size check (file_size_bytes is null or file_size_bytes >= 0),
    constraint ck_media_references_checksum_sha256 check (
        checksum_sha256 is null or length(checksum_sha256) = 64
    )
);

create index idx_media_references_owner
    on media_references (owner_type, owner_id);

create index idx_media_references_purpose
    on media_references (purpose);
