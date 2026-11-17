import { test, expect } from '@playwright/test';
import { API_URL, authedHeaders, assertOk } from '../../fixtures/api';
import { token } from '../../fixtures/context';
import * as allure from 'allure-js-commons';

test.describe('Users & Invitations', () => {
  let inviteId: string;
  let createdUserId: string;
  let createdUserPhone: string;

  test('create invitation', async ({ request }) => {
    await allure.label('suite', 'Users');
    const res = await request.post(`${API_URL}/api/v1/invites`, {
      headers: authedHeaders(token()),
      data: {
        email: `e2e-invited-${Date.now()}@test.manejoprime.com.br`,
        role: 'worker',
      },
    });
    await assertOk(res, 'POST /invites');
    const body = await res.json();
    inviteId = body.id ?? body.inviteId;
    expect(inviteId).toBeTruthy();
  });

  test('list invitations', async ({ request }) => {
    const res = await request.get(`${API_URL}/api/v1/invites`, {
      headers: authedHeaders(token()),
    });
    await assertOk(res, 'GET /invites');
    const body = await res.json();
    const items = Array.isArray(body) ? body : (body.items ?? body);
    expect(Array.isArray(items)).toBe(true);
  });

  test('revoke invitation', async ({ request }) => {
    test.skip(!inviteId, 'Invite not created');
    const res = await request.delete(`${API_URL}/api/v1/invites/${inviteId}`, {
      headers: authedHeaders(token()),
    });
    expect([200, 204]).toContain(res.status());
  });

  test('create user (direct)', async ({ request }) => {
    const res = await request.post(`${API_URL}/api/v1/users`, {
      headers: authedHeaders(token()),
      data: {
        name: 'Funcionário E2E',
        phone: `55119${Date.now().toString().slice(-8)}`,
        role: 'worker',
      },
    });
    await assertOk(res, 'POST /users');
    const body = await res.json();
    createdUserId = body.id;
    createdUserPhone = body.phone;
    expect(createdUserId).toBeTruthy();
  });

  test('list users', async ({ request }) => {
    const res = await request.get(`${API_URL}/api/v1/users`, {
      headers: authedHeaders(token()),
    });
    await assertOk(res, 'GET /users');
    const body = await res.json();
    const items = body.items ?? body;
    expect(Array.isArray(items)).toBe(true);
  });

  test('update user', async ({ request }) => {
    test.skip(!createdUserId, 'User not created');
    const res = await request.patch(`${API_URL}/api/v1/users/${createdUserId}`, {
      headers: authedHeaders(token()),
      data: { name: 'Funcionário E2E Atualizado', phone: createdUserPhone, role: 'worker' },
    });
    await assertOk(res, 'PATCH /users/:id');
  });

  test('deactivate user', async ({ request }) => {
    test.skip(!createdUserId, 'User not created');
    const res = await request.post(`${API_URL}/api/v1/users/${createdUserId}/deactivate`, {
      headers: authedHeaders(token()),
    });
    await assertOk(res, 'POST /users/:id/deactivate');
  });

  test('reactivate user', async ({ request }) => {
    test.skip(!createdUserId, 'User not created');
    const res = await request.post(`${API_URL}/api/v1/users/${createdUserId}/activate`, {
      headers: authedHeaders(token()),
    });
    await assertOk(res, 'POST /users/:id/activate');
  });

  test('update profile', async ({ request }) => {
    const res = await request.patch(`${API_URL}/api/v1/profile`, {
      headers: authedHeaders(token()),
      data: { name: 'E2E Admin Atualizado' },
    });
    await assertOk(res, 'PATCH /profile');
  });
});
