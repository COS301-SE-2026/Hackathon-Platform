import { expect, Locator, Page } from '@playwright/test';

export class HomePage {
  readonly page: Page;
  readonly registeredTab: Locator;
  readonly upcomingTab: Locator;
  readonly completedTab: Locator;
  readonly searchInput: Locator;
  readonly eventsCount: Locator;
  readonly eventCards: Locator;

  constructor(page: Page) {
    this.page = page;
    this.registeredTab = page.locator('nav.tabs a.tab').filter({ hasText: 'Registered' });
    this.upcomingTab = page.locator('nav.tabs a.tab').filter({ hasText: 'Upcoming' });
    this.completedTab = page.locator('nav.tabs a.tab').filter({ hasText: 'Completed' });
    this.searchInput = page.locator('input[type="search"][placeholder="Search events"]');
    this.eventsCount = page.locator('.events-count');
    this.eventCards = page.locator('.event-card');
  }

  async goto(): Promise<void> {
    await this.page.goto('/participant/home');
    await expect(this.page).toHaveURL(/\/participant\/home(?:[/?#]|$)/);
  }

  async openUpcoming(): Promise<void> {
    await this.upcomingTab.click();
    await expect(this.upcomingTab).toHaveClass(/active/);
    await expect(this.eventsCount).toBeVisible();
  }

  eventCard(name: string): Locator {
    return this.eventCards.filter({ hasText: name });
  }

  async search(term: string): Promise<void> {
    await this.searchInput.fill(term);
  }

  async openEvent(name: string): Promise<void> {
    await this.eventCard(name).click();
  }
}
