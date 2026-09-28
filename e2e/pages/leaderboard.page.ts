import { Locator, Page } from '@playwright/test'

export class LeaderboardPage {
    readonly page: Page;
    readonly tableRows: Locator;
    readonly emptyState: Locator;

    constructor(page: Page) {
        this.page = page;
        this.tableRows = page.locator('.rankings-section table tbody tr');
        this.emptyState = page.getByText('Leaderboard Currently Unavailable');
    }

    async gotoLeaderboardTab(eventId: string): Promise<void> {
        await this.page.goto(`/participant/events/${eventId}?tab=leaderboard`);
    }

    rowForTeam(teamName: string): Locator {
        return this.tableRows.filter({ hasText: teamName });
    }
}
