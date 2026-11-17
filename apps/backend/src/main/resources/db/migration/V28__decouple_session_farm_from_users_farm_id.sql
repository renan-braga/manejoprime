-- Farm-switch updates users.farm_id, but that field is referenced by many data tables
-- (whatsapp_inbound_messages, weighings, medications, etc.) without ON UPDATE CASCADE,
-- so the update is rejected by PostgreSQL.
--
-- Fix: drop the composite FKs on auth_sessions and refresh_tokens that chain through
-- users(farm_id, id). Simple FKs to users(id) and farms(id) already exist from V3.
-- Cross-farm session isolation is now enforced at the application layer:
-- FarmSwitchService checks user_farm_memberships before issuing any session.

alter table refresh_tokens drop constraint fk_refresh_tokens_session_id_farm_id_user_id;
alter table refresh_tokens drop constraint fk_refresh_tokens_farm_id_user_id;
alter table auth_sessions drop constraint fk_auth_sessions_farm_id_user_id;

-- Drop unique constraint that existed solely to support the triple-composite FK above.
alter table auth_sessions drop constraint uq_auth_sessions_id_farm_id_user_id;
