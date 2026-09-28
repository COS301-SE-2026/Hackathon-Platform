import { expect, Locator, Page } from '@playwright/test'

export class HomePage {
    readonly page: Page;
    readonly regTab: Locator;
    readonly upcomingTab: Locator;
    readonly completedTab: Locator;
    readonly searchInput: Locator;
    readonly eventsCount: Locator;
    readonly eventCards: Locator;

    constructor(page: Page) {
        this.page = page;
        this.regTab = page.locator('nav.tabs a.tab', { hasText: 'Registered' });
        this.upcomingTab = page.locator('nav.tabs a.tab', { hasText: 'Upcoming' });
        this.completedTab = page.locator('nav.tabs a.tab', { hasText: 'Completed' });
        this.searchInput = page.getByPlaceholder('Search events');
        this.eventsCount = page.locator('.events-count');
        this.eventCards = page.locator('.event-card')
    }

    async goto(): Promise<void> {
        await this.page.goto('/participant/home');
    }

    async gtoUpcoming(): Promise<void> {
        await this.upcomingTab.click();
        await expect(this.eventsCount).toBeVisible();
    }

    eventCardByName(name: string): Locator {
        return this.eventCards.filter({ hasText: name });
    }

    async searchFor(term: string): Promise<void> {
        return this.searchInput.fill(term);
    }

    async openEvent(name: string): Promise<void> {
        await this.eventCardByName(name).click();
    }
}