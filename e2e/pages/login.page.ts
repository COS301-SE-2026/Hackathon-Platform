import { expect, Locator, Page } from '@playwright/test';

export class LoginPage {
  readonly page: Page;

  readonly emailInput: Locator;
  readonly passwordInput: Locator;

  readonly loginButton: Locator;
  readonly signinButton: Locator;

  readonly signupLink: Locator;
  readonly googleButton: Locator;
  readonly resendVerification: Locator;
  readonly toast: Locator;

  constructor(page: Page) {
    this.page = page;

    // These selectors already worked against your Angular components.
    this.emailInput = page.locator('input#email');
    this.passwordInput = page.locator('input#password');

    this.loginButton = page.locator('form button[type="submit"]');

    // Alias expected by the existing dev tests.
    this.signinButton = this.loginButton;

    this.signupLink = page.getByRole('link', {
      name: 'Register',
      exact: true,
    });

    this.googleButton = page.locator('.google-button');

    this.resendVerification = page.locator('.resend-button');

    // PrimeNG toast message.
    this.toast = page.locator('.p-toast-message');
  }

  async goto(): Promise<void> {
    await this.page.goto('/login', {
      waitUntil: 'domcontentloaded',
    });

    await expect(this.page).toHaveURL(/\/login(?:[/?#]|$)/);
    await expect(this.emailInput).toBeVisible();
    await expect(this.passwordInput).toBeVisible();
  }

  async fill(email: string, password: string): Promise<void> {
    await this.emailInput.fill(email);
    await this.passwordInput.fill(password);
  }

  async login(email: string, password: string): Promise<void> {
    await this.fill(email, password);
    await this.loginButton.click();
  }

  async expectOnLogin(): Promise<void> {
    await expect(this.page).toHaveURL(/\/login(?:[/?#]|$)/);
  }
}