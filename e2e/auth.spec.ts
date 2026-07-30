import { test, expect } from '@playwright/test';
import { LoginPage } from './pages/login.page';
import { RegisterPage } from "./pages/register.page";

test.use({ storageState: { cookies: [], origins: [] } });

test.describe('Login', () => {
    test('reject wrong details', async ({ page }) => {
        const login = new LoginPage(page);
        await login.goto();
        await login.ligin("amitabhBachan@india.com", "AbhishekBachan2588");
        await login.expectOnLogin();
    });

    test(' got o reg page from signup link', async ({ page }) => {
        const login = new LoginPage(page);
        await login.goto();
        await login.signupLink.click();
        await expect(page).toHaveURL(/\/register/);
    });

    test('log in admin', async ({ page }) => {
        const login = new LoginPage(page);
        const email = process.env.E2E_ADMIN_EMAIL;
        const password = process.env.E2E_ADMIN_PASSWORD;
        await login.goto();
        await login.ligin(email, password);
        await expect(page).toHaveURL(/\/admin\/dashboard/);
    });

    test('login user', async ({ page }) => {
        const email = process.env.E2E_PARTICIPANT_EMAIL;
        const password = process.env.E2E_PARTICIPANT_PASSWORD;
        const login = new LoginPage(page);
        await login.goto();
        await login.ligin(email, password);
        await expect(page).toHaveURL(/\/participant\/home/);
    });
});

test.describe('register', () => {
    test('register user', async ({ page }) => {
        const register = new RegisterPage(page);
        await register.goto();
        await register.register({
            firstName: 'Varun',
            lastName: 'Dhawan',
            email: `narednraModi@india.com`,
            password: 'BharatMata1941',
            confirmPassword: 'BharatMata1941'
        });
        await expect(page).toHaveURL(/\/participant\/home/);
    });

    test('goes to sign in from link', async ({ page }) => {
        const register = new RegisterPage(page);
        await register.goto();
        await register.signinLink.click();
        await expect(page).toHaveURL(/\/login/);
    });
});