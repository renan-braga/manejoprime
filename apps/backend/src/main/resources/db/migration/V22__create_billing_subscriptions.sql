create table billing_subscriptions (
    id uuid primary key,
    farm_id uuid not null,
    provider varchar(30),
    provider_customer_id varchar(120),
    provider_subscription_id varchar(120),
    plan varchar(30) not null,
    status varchar(30) not null,
    current_period_end timestamp with time zone,
    grace_until timestamp with time zone,
    suspended_at timestamp with time zone,
    canceled_at timestamp with time zone,
    billing_exempt boolean not null default false,
    exemption_reason varchar(255),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uq_billing_subscriptions_farm_id unique (farm_id),
    constraint fk_billing_subscriptions_farm_id foreign key (farm_id) references farms (id),
    constraint ck_billing_subscriptions_plan check (
        plan in ('free', 'starter', 'pro', 'business', 'enterprise', 'internal')
    ),
    constraint ck_billing_subscriptions_status check (
        status in ('free', 'trialing', 'active', 'past_due', 'suspended', 'canceled', 'payment_required')
    )
);

create index idx_billing_subscriptions_farm_id_status on billing_subscriptions (farm_id, status);
create index idx_billing_subscriptions_provider_subscription_id
    on billing_subscriptions (provider, provider_subscription_id);

create table billing_webhook_events (
    id uuid primary key,
    provider varchar(30) not null,
    provider_event_id varchar(160) not null,
    event_type varchar(80) not null,
    payload text not null,
    processed_at timestamp with time zone,
    created_at timestamp with time zone not null,
    constraint uq_billing_webhook_events_provider_event unique (provider, provider_event_id)
);

create index idx_billing_webhook_events_processed_at on billing_webhook_events (processed_at);
