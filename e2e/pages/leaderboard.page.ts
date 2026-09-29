import { expect, Locator, Page } from '@playwright/test';
import { openParticipantEventTab } from '../utils/navigation';

export class LeaderboardPage {
  readonly page: Page;
  readonly tableRows: Locator;

  constructor(page: Page) {
    this.page = page;
    this.tableRows = page.locator('.rankings-section table tbody tr');
  }

  async goto(eventId: string): Promise<void> {
    // The visible tab is labelled "Rankings" even though the query-param value
    // and component are named leaderboard.
    await openParticipantEventTab(this.page, eventId, 'Rankings', 'leaderboard');
    await expect(this.page.getByRole('heading', { name: 'Leaderboard', exact: true })).toBeVisible({
      timeout: 20_000,
    });
  }

  rowForTeam(teamName: string): Locator {
    return this.tableRows.filter({ hasText: teamName });
  }

  async expectTeamScore(teamName: string, score: number): Promise<void> {
    const row = this.rowForTeam(teamName);
    await expect(row).toBeVisible({ timeout: 20_000 });
    await expect(row).toContainText(`${score.toFixed(2)} pts`);
  }
}
