import { test, expect } from '@playwright/test';
import { randomEmail, VALID_PASSWORD } from './fixtures/test-data';
import { MAILPIT_URL, waitForVerificationToken } from './fixtures/mailpit';
import { RegisterPage } from './pages/register.page';
import { LoginPage } from './pages/login.page';

test.use({ storageState: { cookies: [], origins: [] }});

test.describe('Landing page', () => {
    test('login button works', async ({ page }) => {
        await page.goto('/');
        await page.getByRole('link', { name: /explore hackathons/i }).click();
        await expect(page).toHaveURL(/\/login$/);
    });

    test('unkown URL goes back to landing page', async({ page }) => {
        await page.goto('/cos-301-takes-holiday');
        await expect(page).toHaveURL(/localhost:4200\/?$/);
        await expect(page.getByRole('link', {name: /explore hackathons/i})).toBeVisible();
    });
});

test.describe('Email verification page', () => {
    test('with no token redirect to verify your email', async ({ page }) => {
        const email = randomEmail('verify_prompt');
        await page.goto(`/verify-email?email=${encodeURIComponent(email)}`);
        await expect(page.getByRole('heading', {name: /check your email/i})).toBeVisible();
        await expect(page.getByText(email)).toBeVisible();
        await expect(page.getByRole('button', {name: /resend verification email/i})).toBeVisible();
        await page.getByRole('link', {name: /log in/i }).click();
        await expect(page).toHaveURL(/\/login$/);
    });


})