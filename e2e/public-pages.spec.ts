import { test, expect } from '@playwright/test';
import { randomEmail, VALID_PASSWORD } from './fixtures/test-data';
import { MAILPIT_URL, waitForVerificationToken } from './fixtures/mailpit';
import { RegisterPage } from './pages/register.page';
import { LoginPage } from './pages/login.page';

test.use({ storageState: { cookies: [], origins: [] }});
const STUB_TOKEN = 'stub.jwt.token';

test.describe('Landing page', () => {
    test('login button works', async ({ page }) => {
        await page.goto('/');
        await page.locator('#home').getByRole('link', { name: /explore hackathons/i }).click();
        await expect(page).toHaveURL(/\/login$/);
    });

    test('unkown URL goes back to landing page', async({ page }) => {
        await page.goto('/cos-301-takes-holiday');
        await expect(page).toHaveURL(/localhost:4200\/?$/);
        await expect(page.locator('#home').getByRole('link', { name: /explore hackathons/i })).toBeVisible();
    });
});

test.describe('Email verification page', () => {
    test('with no token redirect to verify your email', async ({ page }) => {
        const email = randomEmail('verify');
        await page.goto(`/verify-email?email=${encodeURIComponent(email)}`);
        await expect(page.getByRole('heading', {name: /check your email/i})).toBeVisible();
        await expect(page.getByText(email)).toBeVisible();
        await expect(page.getByRole('button', {name: /resend verification email/i})).toBeVisible();
        await page.getByRole('link', {name: /log in/i }).click();
        await expect(page).toHaveURL(/\/login$/);
    });

    test('resend from the prompt calls API with the address and confirms', async ({ page }) => {
        const email = randomEmail('verify_resend');
        let sentTo = '';
        await page.route('**/api/auth/resend-verification*', async (route) => {
            sentTo = new URL(route.request().url()).searchParams.get('email') ?? '';
            await route.fulfill({ status: 204 });
        });
        await page.goto(`/verify-email?email=${encodeURIComponent(email)}`);
        await page.getByRole('button', { name: /resend verification email/i }).click();
        await expect(page.locator('.p-toast-message').filter({ hasText: /email sent/i })).toBeVisible();
        expect(sentTo).toBe(email);
    });

    test('an invalid token shows verification failed', async ({ page }) => {
        const verifyCall = page.waitForResponse((r) => r.url().includes('/api/auth/verify-email'));
        await page.goto('/verify-email?token=this-token-does-not-exist');
        expect((await verifyCall).status(), 'API should reject an unknown token').toBeGreaterThanOrEqual(400);
        await expect(page.getByRole('heading', { name: /verification failed/i })).toBeVisible({ timeout: 10_000 });
        await page.getByRole('link', { name: /back to login/i }).click();
        await expect(page).toHaveURL(/\/login$/);
    });

    test('a valid token verifies, stores the session and redirects', async ({ page }) => {
        await page.route('**/api/auth/verify-email*', (route) =>
            route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({
                    token: STUB_TOKEN, userId: 'u-1', firstName: 'Stub', lastName: 'User',
                    email: 'plzwork@e2e-test.com', role: 'PARTICIPANT', emailVerified: true, message: 'Verified',
                }),
            }),
        );
        await page.goto('/verify-email?token=stub-token');
        await expect(page.getByRole('heading', { name: /email verification complete/i })).toBeVisible();
        await expect(page).toHaveURL(/\/participant\/home/, { timeout: 10_000 }); // redirect after ~800ms
        expect(await page.evaluate(() => localStorage.getItem('token'))).toBe(STUB_TOKEN);
    });
});

test.describe('Google OAuth return page', () => {
    test('no token sends the user back to login with an error', async ({ page }) => {
        await page.goto('/auth/oauth-success');
        await expect(page).toHaveURL(/\/login\?error=google_login_failed/, { timeout: 10_000 });
        expect(await page.evaluate(() => localStorage.getItem('token'))).toBeNull();
    });
});

test.describe('Full registration flow', () => {
    test('a new user registers, clicks the emailed link, is logged in, and can log in again', async ({ page }) => {
        const email = randomEmail('journey');
        const register = new RegisterPage(page);
        await register.goto();
        await register.register({ firstName: 'Real', lastName: 'Journey', email, password: VALID_PASSWORD });
        await expect(page).toHaveURL(/\/verify-email\?email=/);
        const token = await waitForVerificationToken(email);
        await page.goto(`/verify-email?token=${encodeURIComponent(token)}`);
        await expect(page.getByRole('heading', { name: /email verification complete/i })).toBeVisible({ timeout: 10_000 });
        await expect(page).toHaveURL(/\/participant\/home/, { timeout: 10_000 });
        await page.evaluate(() => localStorage.clear());
        const login = new LoginPage(page);
        await login.goto();
        await login.login(email, VALID_PASSWORD);
        await expect(page).toHaveURL(/\/participant\/home/, { timeout: 15_000 });
    });
});
