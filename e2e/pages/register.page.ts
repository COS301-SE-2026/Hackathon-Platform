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
        this.firstName = page.locator('#firstName');
        this.lastName = page.locator('#lastName');
        this.email = page.locator('#email');
        this.password = page.locator('#password');
        this.confirmPassword = page.locator('#confirmPassword');
        this.createAccBtn = page.getByRole('button', {
            name: /create account/i,
    });
        this.signinLink = page.getByRole('link', {
            name: /sign in/i,
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
        this.createAccBtn.click();
    }

    async touchAll() {
        for(const f of [this.firstName, this.lastName, this.email, this.password, this.confirmPassword]) {
            await f.focus();
            await f.blur();
        }
    }
}