import { expect, Locator, Page } from '@playwright/test';

export class EventDetailsPage {
  readonly page: Page;
  readonly registerButton: Locator;
  readonly registrationDialog: Locator;
  readonly registrationKeyInput: Locator;
  readonly confirmRegisterButton: Locator;
  readonly toast: Locator;

  constructor(page: Page) {
    this.page = page;
    this.registerButton = page.getByRole('button', { name: /^(Register|Registered)$/ });

    // Do not assert visibility on <app-modal> itself. The custom-element host has
    // no rendered box, while its fixed .modal-overlay/dialog children are visible.
    this.registrationDialog = page
      .locator('.modal-overlay')
      .filter({ hasText: 'Register for Event' });

    this.registrationKeyInput = page.locator('input#registration-key');
    this.confirmRegisterButton = this.registrationDialog.getByRole('button', {
      name: /^(Register|Registering\.\.\.)$/,
    });
    this.toast = page.locator('.p-toast-message').last();
  }

  async goto(eventId: string, tab = 'overview'): Promise<void> {
    await this.page.goto(`/participant/events/${eventId}?tab=${tab}`, {
      waitUntil: 'domcontentloaded',
    });
    await expect(this.page).toHaveURL(new RegExp(`/participant/events/${eventId}`));
    await expect(this.registerButton).toBeVisible();
  }

  async openRegistrationDialog(): Promise<void> {
    await expect(this.registerButton).toBeEnabled();
    await this.registerButton.click();
    await expect(this.registrationDialog).toBeVisible();
    await expect(
      this.registrationDialog.getByRole('heading', {
        name: 'Register for Event',
        exact: true,
      }),
    ).toBeVisible();
  }

  async registerPublic(): Promise<void> {
    await this.openRegistrationDialog();
    await this.confirmRegisterButton.click();
  }

  async registerPrivate(key: string): Promise<void> {
    await this.openRegistrationDialog();
    await expect(this.registrationKeyInput).toBeVisible();
    await this.registrationKeyInput.fill(key);
    await this.confirmRegisterButton.click();
  }

  async submitPrivateKey(key: string): Promise<void> {
    await expect(this.registrationDialog).toBeVisible();
    await expect(this.registrationKeyInput).toBeVisible();
    await this.registrationKeyInput.fill(key);
    await this.confirmRegisterButton.click();
  }

  async expectRegistered(): Promise<void> {
    await expect(this.registrationDialog).toBeHidden();
    await expect(this.registerButton).toBeDisabled();
    await expect(this.registerButton).toHaveText('Registered');
  }
}
