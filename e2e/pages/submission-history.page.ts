import { expect, Locator, Page } from '@playwright/test';
import { openParticipantEventTab } from '../utils/navigation';

export class SubmissionHistoryPage {
  readonly page: Page;
  readonly tableRows: Locator;

  constructor(page: Page) {
    this.page = page;
    this.tableRows = page.locator('.submission-history table tbody tr');
  }

  async goto(eventId: string): Promise<void> {
    await openParticipantEventTab(this.page, eventId, 'History', 'submission-history');
    await expect(this.page.getByRole('heading', { name: 'Submission History', exact: true })).toBeVisible({
      timeout: 20_000,
    });
  }

  rowForLevel(levelLabel: string): Locator {
    return this.tableRows.filter({ hasText: levelLabel });
  }

  async expectScored(levelLabel: string, score: number): Promise<void> {
    const row = this.rowForLevel(levelLabel).first();
    await expect(row).toBeVisible();
    await expect(row).toContainText('Completed');
    await expect(row).toContainText(new RegExp(`\\b${score}(?:\\.0+)?\\b`));
  }

  async expectSubmissionCountAtLeast(count: number): Promise<void> {
    await expect
      .poll(async () => this.tableRows.count(), {
        timeout: 20_000,
      })
      .toBeGreaterThanOrEqual(count);
  }
}
