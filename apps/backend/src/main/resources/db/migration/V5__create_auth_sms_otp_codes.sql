create table sms_otp_codes (
    id uuid primary key,
    farm_id uuid not null,
    user_id uuid not null,
    phone varchar(32) not null,
    code varchar(6) not null,
    expires_at timestamp with time zone not null,
    consumed_at timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint chk_sms_otp_codes_code_length check (char_length(code) = 6),
    constraint fk_sms_otp_codes_farm_id foreign key (farm_id) references farms (id),
    constraint fk_sms_otp_codes_farm_id_user_id foreign key (farm_id, user_id) references users (farm_id, id)
);

create index idx_sms_otp_codes_farm_id on sms_otp_codes (farm_id);
create index idx_sms_otp_codes_user_id on sms_otp_codes (user_id);
create index idx_sms_otp_codes_phone_code on sms_otp_codes (phone, code);
create index idx_sms_otp_codes_expires_at on sms_otp_codes (expires_at);
