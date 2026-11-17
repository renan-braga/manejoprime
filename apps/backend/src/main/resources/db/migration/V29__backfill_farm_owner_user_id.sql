-- Farms created before V25 have owner_user_id = NULL.
-- The primary farm for each user is stored in users.farm_id; use it to
-- backfill owner_user_id so that BillingAccessGuard.findByAccountOwnerFarmId
-- can resolve the active subscription across all farms of the same account.
update farms
set owner_user_id = (
    select u.id from users u
    where u.farm_id = farms.id
    order by u.created_at asc
    limit 1
)
where owner_user_id is null;
