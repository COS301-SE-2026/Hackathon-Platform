import { Page, Locator, expect } from '@playwright/test';

export class RegisterPage {
    readonly page: Page;
    readonly firstName: Locator;
    readonly lastName: Locator;
    readonly email: Locator;
    readonly password: Locator;
    readonly confirmPassword: Locator;
    readonly createAccBtn: Locator;
    readonly signinLink: Locator;
    readonly toast: Locator;

    constructor(page: Page) {
        this.page = page;
        this.firstName = page.locator('input#firstName');
        this.lastName = page.locator('input#lastName');
        this.email = page.locator('input#email');
        this.password = page.locator('input#password');
        this.confirmPassword = page.locator('input#confirmPassword');
        this.createAccBtn = page.getByRole('button', {
            name: /create account/i,
    });
        this.signinLink = page.getByRole('link', {
            name: /log in/i,
        })
        this.toast = page.locator('.p-toast-message');
    }

    async goto() {
        await this.page.goto('/register');
        await expect(this.firstName).toBeVisible();
    }

    async fillForm(opts:{ firstName: string, lastName: string, email: string, password: string, confirmPassword?: string }) {
        await this.firstName.fill(opts.firstName);
        await this.lastName.fill(opts.lastName);
        await this.email.fill(opts.email);
        await this.password.fill(opts.password);
        await this.confirmPassword.fill(opts.confirmPassword ?? opts.password);
    }

    async register(opts:{
        firstName: string;
        lastName: string;
        email: string;
        password: string;
        confirmPassword?: string;
    }){
        await this.fillForm(opts);
        await this.createAccBtn.click();
    }

    async touchAll() {
        for(const f of [this.firstName, this.lastName, this.email, this.password, this.confirmPassword]) {
            await f.focus();
            await f.blur();
        }
    }
}