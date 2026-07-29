import { Page, Locator } from '@playwright/test';

export class RegisterPage {
    readonly page: Page;
    readonly firstName: Locator;
    readonly lastName: Locator;
    readonly email: Locator;
    readonly password: Locator;
    readonly confirmPassword: Locator;
    readonly createAccBtn: Locator;
    readonly signinLink: Locator;
    readonly errBanner: Locator;
    readonly fieldErr: Locator;

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
        this.errBanner = page.locator('.error-banner');
        this.fieldErr = page.locator('.field-error');
        this.signinLink = page.getByRole('link', {
            name: /sign in/i,
        })

    }
}