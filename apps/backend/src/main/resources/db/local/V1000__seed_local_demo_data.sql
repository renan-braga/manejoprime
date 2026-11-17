insert into farms (
    id,
    nome,
    localizacao,
    area_hectares,
    rendimento_carcaca,
    created_at,
    updated_at
) values (
    '11111111-1111-1111-1111-111111111111',
    'Fazenda ManejoPrime Demo',
    'Ribeirao Preto, SP',
    320.00,
    54.50,
    NOW(),
    NOW()
);

-- Backfill owner after users are inserted (done via update below)


insert into lots (
    id,
    farm_id,
    nome,
    finalidade,
    pasto_area,
    created_at,
    updated_at
) values
(
    '22222222-2222-2222-2222-222222222221',
    '11111111-1111-1111-1111-111111111111',
    'Engorda 1',
    'fattening',
    'Pasto Norte',
    NOW(),
    NOW()
),
(
    '22222222-2222-2222-2222-222222222222',
    '11111111-1111-1111-1111-111111111111',
    'Cria 1',
    'breeding',
    'Pasto Sul',
    NOW(),
    NOW()
);

insert into users (
    id,
    farm_id,
    telefone,
    nome,
    email,
    role,
    ativo,
    created_at,
    updated_at
) values
(
    '33333333-3333-3333-3333-333333333331',
    '11111111-1111-1111-1111-111111111111',
    '5511900000000',
    'Renan Admin',
    'admin@demo.manejoprime.dev',
    'admin',
    true,
    NOW(),
    NOW()
),
(
    '33333333-3333-3333-3333-333333333332',
    '11111111-1111-1111-1111-111111111111',
    '5511999992000',
    'Worker Demo',
    'worker.demo@curralzap.local',
    'worker',
    true,
    NOW(),
    NOW()
);

insert into animals (
    id,
    farm_id,
    lot_id,
    identificacao,
    nome,
    raca,
    sexo,
    data_nascimento,
    peso_nascimento,
    dam_id,
    sire_id,
    status,
    data_entrada,
    created_at,
    updated_at
) values
(
    '44444444-4444-4444-4444-444444444441',
    '11111111-1111-1111-1111-111111111111',
    '22222222-2222-2222-2222-222222222221',
    '347',
    'Trovao',
    'Nelore',
    'macho',
    date '2025-01-01',
    32.50,
    null,
    null,
    'ativo',
    date '2025-03-01',
    NOW(),
    NOW()
),
(
    '44444444-4444-4444-4444-444444444442',
    '11111111-1111-1111-1111-111111111111',
    '22222222-2222-2222-2222-222222222222',
    '512',
    'Aurora',
    'Angus',
    'femea',
    date '2024-10-15',
    29.80,
    null,
    null,
    'ativo',
    date '2024-12-01',
    NOW(),
    NOW()
),
(
    '44444444-4444-4444-4444-444444444443',
    '11111111-1111-1111-1111-111111111111',
    '22222222-2222-2222-2222-222222222221',
    '610',
    'Brisa',
    'Nelore',
    'femea',
    date '2024-07-20',
    30.20,
    null,
    null,
    'ativo',
    date '2024-09-10',
    NOW(),
    NOW()
);

-- Backfill farm owner and membership for demo farm
update farms set owner_user_id = '33333333-3333-3333-3333-333333333331'
where id = '11111111-1111-1111-1111-111111111111';

insert into user_farm_memberships (user_id, farm_id, role)
values ('33333333-3333-3333-3333-333333333331', '11111111-1111-1111-1111-111111111111', 'admin');

-- Segunda fazenda demo (dados diferentes da primeira)
insert into farms (
    id, nome, localizacao, area_hectares, rendimento_carcaca, created_at, updated_at
) values (
    '55555555-5555-5555-5555-555555555555',
    'Fazenda Boa Vista Demo',
    'Rondonópolis, MT',
    1240.00,
    52.00,
    DATEADD('DAY', -60, NOW()),
    DATEADD('DAY', -60, NOW())
);

insert into lots (
    id, farm_id, nome, finalidade, pasto_area, created_at, updated_at
) values
(
    '66666666-6666-6666-6666-666666666661',
    '55555555-5555-5555-5555-555555555555',
    'Recria A',
    'raising',
    'Pasto Leste',
    DATEADD('DAY', -60, NOW()), DATEADD('DAY', -60, NOW())
),
(
    '66666666-6666-6666-6666-666666666662',
    '55555555-5555-5555-5555-555555555555',
    'Matrizes',
    'reproduction',
    'Pasto Oeste',
    DATEADD('DAY', -60, NOW()), DATEADD('DAY', -60, NOW())
),
(
    '66666666-6666-6666-6666-666666666663',
    '55555555-5555-5555-5555-555555555555',
    'Engorda Final',
    'fattening',
    'Pasto Central',
    DATEADD('DAY', -60, NOW()), DATEADD('DAY', -60, NOW())
);

insert into animals (
    id, farm_id, lot_id, identificacao, nome, raca, sexo,
    data_nascimento, peso_nascimento, dam_id, sire_id,
    status, data_entrada, created_at, updated_at
) values
(
    '77777777-7777-7777-7777-777777777771',
    '55555555-5555-5555-5555-555555555555',
    '66666666-6666-6666-6666-666666666662',
    'BV-001',
    'Guaraná',
    'Nelore PO',
    'femea',
    date '2021-03-10',
    34.00,
    null, null,
    'ativo',
    date '2021-05-01',
    DATEADD('DAY', -60, NOW()), DATEADD('DAY', -60, NOW())
),
(
    '77777777-7777-7777-7777-777777777772',
    '55555555-5555-5555-5555-555555555555',
    '66666666-6666-6666-6666-666666666662',
    'BV-002',
    'Serena',
    'Senepol',
    'femea',
    date '2020-08-22',
    31.50,
    null, null,
    'ativo',
    date '2020-10-15',
    DATEADD('DAY', -60, NOW()), DATEADD('DAY', -60, NOW())
),
(
    '77777777-7777-7777-7777-777777777773',
    '55555555-5555-5555-5555-555555555555',
    '66666666-6666-6666-6666-666666666662',
    'BV-003',
    'Primavera',
    'Nelore PO',
    'femea',
    date '2021-11-05',
    33.20,
    null, null,
    'ativo',
    date '2022-01-20',
    DATEADD('DAY', -60, NOW()), DATEADD('DAY', -60, NOW())
),
(
    '77777777-7777-7777-7777-777777777774',
    '55555555-5555-5555-5555-555555555555',
    '66666666-6666-6666-6666-666666666661',
    'BV-101',
    null,
    'Nelore PO',
    'macho',
    date '2024-02-14',
    38.00,
    '77777777-7777-7777-7777-777777777771',
    null,
    'ativo',
    date '2024-04-01',
    DATEADD('DAY', -60, NOW()), DATEADD('DAY', -60, NOW())
),
(
    '77777777-7777-7777-7777-777777777775',
    '55555555-5555-5555-5555-555555555555',
    '66666666-6666-6666-6666-666666666661',
    'BV-102',
    null,
    'Senepol x Nelore',
    'femea',
    date '2024-05-30',
    29.50,
    '77777777-7777-7777-7777-777777777772',
    null,
    'ativo',
    date '2024-07-10',
    DATEADD('DAY', -60, NOW()), DATEADD('DAY', -60, NOW())
),
(
    '77777777-7777-7777-7777-777777777776',
    '55555555-5555-5555-5555-555555555555',
    '66666666-6666-6666-6666-666666666663',
    'BV-201',
    'Sultão',
    'Nelore PO',
    'macho',
    date '2022-06-18',
    36.00,
    null, null,
    'ativo',
    date '2023-09-01',
    DATEADD('DAY', -60, NOW()), DATEADD('DAY', -60, NOW())
),
(
    '77777777-7777-7777-7777-777777777777',
    '55555555-5555-5555-5555-555555555555',
    '66666666-6666-6666-6666-666666666663',
    'BV-202',
    'Corisco',
    'Senepol',
    'macho',
    date '2022-09-03',
    35.50,
    null, null,
    'ativo',
    date '2023-11-15',
    DATEADD('DAY', -60, NOW()), DATEADD('DAY', -60, NOW())
);

update farms set owner_user_id = '33333333-3333-3333-3333-333333333331'
where id = '55555555-5555-5555-5555-555555555555';

insert into user_farm_memberships (user_id, farm_id, role)
values ('33333333-3333-3333-3333-333333333331', '55555555-5555-5555-5555-555555555555', 'admin');

-- Assinatura business/active para a farm demo (simula usuário com plano pago)
insert into billing_subscriptions (
    id, farm_id, provider, provider_customer_id, provider_subscription_id,
    plan, status, current_period_end, billing_exempt,
    created_at, updated_at
) values (
    'aaaaaaaa-aaaa-4aaa-aaaa-aaaaaaaaaaaa',
    '11111111-1111-1111-1111-111111111111',
    'asaas',
    'cus_local_demo',
    'sub_local_demo',
    'business',
    'active',
    DATEADD('DAY', 30, NOW()),
    false,
    NOW(),
    NOW()
);
