import { expect, Locator, Page } from '@playwright/test'

export class EventDetailsPage {
    readonly page: Page;
    readonly regButton: Locator;
    readonly regModal: Locator;
    readonly regKeyInput: Locator;
    readonly modelConfirmButton: Locator;
    readonly modelCancelButton: Locator;
    readonly toast: Locator;

    constructor(page: Page) {
        this.page = page;
        this.regButton = page.getByRole('button', { name: /^Register(ed)>$/ });
        this.regModal = page.locator('app-model').filter({ hasText: 'Registered for Event '});
        this.regKeyInput = page.getByLabel('Registration Key');
        this.modelConfirmButton = this.regModal.getByRole('button', { name: /^Register(ing\.\.\.)?$/ });
        this.modelCancelButton = this.regModal.getByRole('button', { name: 'Cancel' });
        this.toast = page.locator('.p-toast-message');
    }

    async goto(eventId: string, tab = 'overview'): Promise<void> {
        await this.page.goto(`/participant/events/${eventId}?tab=${tab}`);
    }

    tab(label: string): Locator {
        return this.page.locator('section.event-tabs: a.tab', {hasText: label });
    }

    async goToTab(label: string): Promise<void> {
        await this.tab(label).click();
    }

    async expectOnOverview(): Promise<void> {
        await expect(this.page).toHaveURL(/tab=overview/);
    }

    async openRegistrationModel(): Promise<void> {
        await this.regButton.click();
        await expect(this.regModal).toBeVisible();
    }

    async registerPublic(): Promise<void> {
        await this.openRegistrationModel();
        await this.modelConfirmButton.click();
    }

    async registerPrivate(key: string): Promise<void> {
        await this.openRegistrationModel();
        await this.regKeyInput.fill(key);
        await this.modelConfirmButton.click();
    }

    async expectRegistered(): Promise<void> {
        await expect(this.regButton).toBeDisabled();
    }

    async expectNotRegistered(): Promise<void> {
        await expect(this.regButton).toBeEnabled();
    }
}