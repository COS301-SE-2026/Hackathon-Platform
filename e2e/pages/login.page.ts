import { Page, Locator, expect} from '@playwright/test';

export class LoginPage{
    readonly page: Page;
    readonly emailInput: Locator;
    readonly passwordInput: Locator;
    readonly signinButton: Locator;
    readonly signupLink: Locator;
    readonly googleButton: Locator;
    readonly resendVerification: Locator;

    constructor(page: Page){
        this.page = page;
        this.emailInput = page.locator('input#email');
        this.passwordInput = page.locator('input#password');
        this.signinButton = page.getByRole('button', { name: "Log in" });
        this.signupLink = page.getByRole('link', { name: 'Register'});
        this.googleButton = page.getByRole('button', { name: /continue with google/i });
        this.resendVerification = page.getByRole('button', { name: /resend verification email/i });
    }

    async goto() {
        await this.page.goto('/login');
        await expect(this.emailInput).toBeVisible();
    }

    async login(email: string, password: string) {
        await this.emailInput.fill(email);
        await this.passwordInput.fill(password);
        await this.signinButton.click();
    }

    async expectOnLogin() {
        await expect(this.page).toHaveURL(/\/login/);
    }
}