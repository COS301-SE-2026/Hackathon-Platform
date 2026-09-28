import { test, expect, Page } from '@playwright/test';
import { LoginPage } from './pages/login.page';
import { RegisterPage } from "./pages/register.page";
import { apiContext, apiRegister } from './fixtures/api';
import { users, randomEmail, VALID_PASSWORD } from './fixtures/test-data'

test.use({ storageState: { cookies: [], origins: [] } });

test.describe('Registration', () =>{
    test('a new user registers and email is sent', async ({ page }) => {
        const register = new RegisterPage(page);
        await register.goto();
        const email = randomEmail('reg_ok');
        await register.fillForm({ fistName: 'John', lastName: 'Cena', email, password: VALID_PASSWORD });
        await expect(register.createAccBtn).toBeEnabled();
        await register.createAccBtn.click();
        await expect(page).toHaveURL(/\/verify-email\?email=/, {timeout: 15_000});
        await expect(page.getByRole('heading', {name: /check your email/i})).toBeVisible();
        await expect(page.getByText(email)).toBeVisible();
        expect(await page.evaluate(() => localStorage.getItem('token'))).toBeNull();
    });

    test('submit stays disabled until all fields are entered', async ({page}) => {
        const register = new RegisterPage(page);
        await register.goto();
        await expect(register.createAccBtn).toBeDisabled();
        await register.fillForm({ firstName: 'Avinash', lastName: 'Singh', email: randomEmail('reg_weak'), password: 'weak' });
        await expect(register.createAccBtn).toBeEnabled();
        await register.password.fill(VALID_PASSWORD);
        await register.confirmPassword.fill(VALID_PASSWORD);
        await expect(register.createAccBtn).toBeEnabled();
    });

    test('password requirement checklist ticks off', async ({ page }) => {
        const register = new RegisterPage(page);
        await register.goto();
        const items = page.locator('.password-requirements li');
        await expect(items).toHaveCount(5);
        await expect(page.locator('.password-requirements li.valid')).toHaveCount(0);
        await register.password.fill(VALID_PASSWORD);
        await expect(page.locator('.password-requirements li.valid')).toHaveCount(5);
    });

    test('required-field messages appear once fields are touched empty', async ({ page }) => {
        const register = new RegisterPage(page);
        await register.goto();
        await register.touchAll();

        await expect(page.getByText('First name is required.')).toBeVisible();
        await expect(page.getByText('Last name is required.')).toBeVisible();
        await expect(page.getByText('Email is required.')).toBeVisible();
        await expect(page.getByText('Password is required.')).toBeVisible();
        await expect(page.getByText('Please confirm your password.')).toBeVisible();
        await expect(register.createAccBtn).toBeDisabled();
    });

    test('mismatched confirmation is flagged and blocks submission', async ({ page }) => {
        const register = new RegisterPage(page);
        await register.goto();
        await register.fillForm({
            firstName: 'Ade', lastName: 'Okafor', email: randomEmail('reg_mismatch'),
            password: VALID_PASSWORD, confirmPassword: VALID_PASSWORD + 'x',
        });
        await register.touchAll();

        await expect(page.getByText('Passwords do not match.')).toBeVisible();
        await expect(register.createAccBtn).toBeDisabled();
    });

    test('an invalid email is flagged and blocks submission', async ({ page }) => {
        const register = new RegisterPage(page);
        await register.goto();
        await register.fillForm({ firstName: 'Priya', lastName: 'Naidoo', email: 'not-an-email', password: VALID_PASSWORD });
        await register.touchAll();

        await expect(page.getByText('Please enter a valid email address.')).toBeVisible();
        await expect(register.createAccBtn).toBeDisabled();
    });

    test('registering an email that already exists shows an error and stays on the form', async ({ page }) => {
        const email = randomEmail('reg_dupe');
        const api = await apiContext();
        const seed = await apiRegister(api, { firstName: 'Existing', lastName: 'User', email, password: VALID_PASSWORD });
        expect(seed.status()).toBe(201);
        await api.dispose();

        const register = new RegisterPage(page);
        await register.goto();
        await register.fillForm({ firstName: 'Second', lastName: 'Attempt', email, password: VALID_PASSWORD });
        await register.createAccBtn.click();

        // Backend answers 409; the UI toasts and must NOT navigate.
        await expect(register.toast.filter({ hasText: /already exists/i })).toBeVisible({ timeout: 10_000 });
        await expect(page).toHaveURL(/\/register$/);
    });

    test('register and login pages link to each other', async ({ page }) => {
        const register = new RegisterPage(page);
        await register.goto();
        await register.signinLink.click();
        await expect(page).toHaveURL(/\/login$/);

        await new LoginPage(page).signupLink.click();
        await expect(page).toHaveURL(/\/register$/);
    });
});