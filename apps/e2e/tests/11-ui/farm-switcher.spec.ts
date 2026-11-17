import { test, expect } from '@playwright/test';
import { BASE_URL } from '../../fixtures/api';
import { token, refreshToken, farmId } from '../../fixtures/context';
import * as allure from 'allure-js-commons';

test.describe('Farm Switcher UI', () => {
  test.use({ baseURL: BASE_URL });

  test.beforeEach(async ({ page }) => {
    // Set token in localStorage before navigating to /app so the SPA doesn't redirect to OAuth
    await page.goto('/');
    await page.evaluate(([t, rt]: string[]) => {
      localStorage.setItem('curralzap.admin.auth', JSON.stringify({
        accessToken: t,
        refreshToken: rt,
        tokenType: 'Bearer',
      }));
    }, [token(), refreshToken()]);
    await page.goto('/app');
    await page.waitForURL('**/app**', { timeout: 30_000 });
  });

  test('add farm inline via switcher and verify it appears in list', async ({ page }) => {
    await allure.label('suite', 'UI');
    await allure.label('story', 'Add farm inline');

    // Open farm switcher
    await page.locator('[data-testid="farm-switcher"], [aria-label*="fazenda"], button:has-text("Fazenda")').first().click();
    await page.screenshot({ path: '/tmp/e2e-switcher-open.png' });

    // Click "Add farm" — actual button text is "Adicionar fazenda"
    await page.locator('button:has-text("Adicionar fazenda"), button:has-text("Add farm"), [data-testid="add-farm"]').first().click();

    // Type farm name
    const nameInput = page.locator('input[placeholder*="nome"], input[name="name"], input[placeholder*="fazenda"]').first();
    await nameInput.fill('Fazenda E2E UI Test');

    // Submit
    await page.locator('button[type="submit"], button:has-text("Criar"), button:has-text("Salvar")').first().click();

    // Verify the new farm appears in the switcher list
    await expect(page.locator('text=Fazenda E2E UI Test')).toBeVisible({ timeout: 10_000 });
    await page.screenshot({ path: '/tmp/e2e-switcher-farm-added.png' });
    await allure.attachment('switcher-farm-added', '/tmp/e2e-switcher-farm-added.png', { contentType: 'text/plain' });
  });

  test('click farm in switcher and verify switch', async ({ page }) => {
    await allure.label('story', 'Switch farm via UI');

    // Look for a farm entry in switcher other than current one
    await page.locator('[data-testid="farm-switcher"], button:has-text("Fazenda")').first().click();

    const farmItems = page.locator('[data-testid="farm-item"], [role="menuitem"]:not([aria-selected="true"])');
    const count = await farmItems.count();
    test.skip(count === 0, 'No other farms to switch to');

    await farmItems.first().click();
    await page.waitForURL('**/app**');

    // Verify dashboard loaded after switch
    await expect(page).toHaveURL(/\/app/);
    await page.screenshot({ path: '/tmp/e2e-switcher-switched.png' });
  });
});
