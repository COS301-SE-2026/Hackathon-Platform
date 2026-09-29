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
        await register.fillForm({ firstName: 'John', lastName: 'Cena', email, password: VALID_PASSWORD });
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
        await expect(register.createAccBtn).toBeDisabled();
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

const token = (page: Page) => page.evaluate(() => localStorage.getItem('token'));

test.describe('Login', () => {
    test('empty form does not call the API or leave the page', async ({ page }) => {
        const login = new LoginPage(page);
        await login.goto();
        let apiCalled = false;
        page.on('request', (r) => { if (r.url().includes('/api/auth/login')) apiCalled = true; });
        await login.signinButton.click();
        await expect(login.toast.filter({ hasText: /enter your email and password/i })).toBeVisible();
        await expect(page).toHaveURL(/\/login$/);
        expect(apiCalled).toBe(false);
    });

    test('wrong credentials gives error toast and no session it stays on login', async ({ page }) => {
        const login = new LoginPage(page);
        await login.goto();
        await login.login(randomEmail('nobody'), 'WrongPassword1!');
        await expect(login.toast.filter({ hasText: /invalid email or password/i })).toBeVisible({ timeout: 10_000 });
        await expect(page).toHaveURL(/\/login$/);
        expect(await token(page)).toBeNull();
        await expect(login.resendVerification).toHaveCount(0);
    });

    test('participant logs in and lands on the participant home with a stored session', async ({ page }) => {
        const login = new LoginPage(page);
        await login.goto();
        await login.login(users.participant.email, users.participant.password);
        await expect(page).toHaveURL(/\/participant\/home/, { timeout: 15_000 });
        expect(await token(page)).toBeTruthy();
        const user = await page.evaluate(() => JSON.parse(localStorage.getItem('user') || '{}'));
        expect(user.role).toBe('PARTICIPANT');
    });

    test('admin logs in and lands on the admin events page with a stored session', async ({ page }) => {
        const login = new LoginPage(page);
        await login.goto();
        await login.login(users.admin.email, users.admin.password);
        await expect(page).toHaveURL(/\/admin\/events/, { timeout: 15_000 });
        expect(await token(page)).toBeTruthy();
        const user = await page.evaluate(() => JSON.parse(localStorage.getItem('user') || '{}'));
        expect(user.role).toBe('ADMIN');
    });

    test('pressing enter in the password field submits the form', async ({ page }) => {
        const login = new LoginPage(page);
        await login.goto();
        await login.fill(users.participant.email, users.participant.password);
        await login.passwordInput.press('Enter');
        await expect(page).toHaveURL(/\/participant\/home/, { timeout: 15_000 });
    });

    test('login email is case insensitive', async ({ page }) => {
        const login = new LoginPage(page);
        await login.goto();
        await login.login(users.participant.email.toUpperCase(), users.participant.password);
        await expect(page).toHaveURL(/\/participant\/home/, { timeout: 15_000 });
    });

    test('unverified account is refused and offered a resend verification', async ({ page }) => {
        const email = randomEmail('unverified');
        const api = await apiContext();
        expect((await apiRegister(api, { firstName: 'Not', lastName: 'Verified', email, password: VALID_PASSWORD })).status()).toBe(201);
        await api.dispose();
        let resendCalledWith = '';
        await page.route('**/api/auth/resend-verification*', async (route) => {
            resendCalledWith = new URL(route.request().url()).searchParams.get('email') ?? '';
            await route.fulfill({ status: 204 });
        });
        const login = new LoginPage(page);
        await login.goto();
        await login.login(email, VALID_PASSWORD);
        await expect(login.toast.filter({ hasText: /verify your email/i })).toBeVisible({ timeout: 10_000 });
        await expect(page).toHaveURL(/\/login$/);
        expect(await token(page)).toBeNull();
        await expect(login.resendVerification).toBeVisible();
        await login.resendVerification.click();
        await expect(login.toast.filter({ hasText: /email sent/i })).toBeVisible();
        expect(resendCalledWith).toBe(email);
    });

    test('logged out visitor hitting an admin route is redirected to login', async ({ page }) => {
        await page.goto('/admin/dashboard');
        await expect(page).toHaveURL(/\/login$/, { timeout: 10_000 });
    });

    test('loggedout visitor hitting the super admin route is redirected to login', async ({ page }) => {
        await page.goto('/super-admin');
        await expect(page).toHaveURL(/\/login$/, { timeout: 10_000 });
    });

    test('login page offers Google sign-in and a link to register', async ({ page }) => {
        const login = new LoginPage(page);
        await login.goto();
        await expect(login.googleButton).toBeVisible();
        await login.signupLink.click();
        await expect(page).toHaveURL(/\/register$/);
    });
});