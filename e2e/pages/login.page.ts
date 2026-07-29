import { Page, Locator, expect} from '@playwright/test';

export class LoginPage{
    readonly page: Page;
    readonly emailInput: Locator;
    readonly passwordInput: Locator;
    readonly signinButton: Locator;
    readonly signupLink: Locator;

    constructor(page: Page){
        this.page = page;
        this.emailInput = page.locator('#email');
        this.passwordInput = page.locator('#password');
        this.signinButton = page.getByRole('button', { name: "Sign in" });
        this.signupLink = page.getByRole('link', { name: 'Register'});
    }

    async goto() {
        await this.page.goto('/login');
    }

    async ligin(email: string, password: string) {
        await this.emailInput.fill(email);
        await this.passwordInput.fill(password);
        await this.signinButton.click();
    }

    async expectOnLogin() {
        await expect(this.page).toHaveURL(/\/login/);
    }
}