-- Users registered before V25 (user_farm_memberships) have no membership rows.
-- Their primary farm is stored in users.farm_id; backfill it here so that
-- my-farms listing and farm-quota counting work correctly for legacy accounts.
insert into user_farm_memberships (user_id, farm_id, role)
select u.id, u.farm_id, u.role
from users u
where not exists (
    select 1
    from user_farm_memberships ufm
    where ufm.user_id = u.id
      and ufm.farm_id = u.farm_id
);
