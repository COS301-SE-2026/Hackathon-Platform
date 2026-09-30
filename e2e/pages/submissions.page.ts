import { expect, Locator, Page } from '@playwright/test';
import { openParticipantEventTab } from '../utils/navigation';

export class SubmissionPage {
  readonly page: Page;
  readonly downloadResourcesButton: Locator;
  readonly sourceUploadInput: Locator;
  readonly outputUploadInput: Locator;
  readonly submitButton: Locator;
  readonly toast: Locator;

  constructor(page: Page) {
    this.page = page;
    this.downloadResourcesButton = page.getByRole('button', {
      name: 'Download Resources',
      exact: true,
    });
    this.sourceUploadInput = page
      .locator('.upload-item')
      .filter({ hasText: 'Solution Archive' })
      .locator('input[type="file"]');
    this.outputUploadInput = page
      .locator('.upload-item')
      .filter({ hasText: 'Output File (JSON)' })
      .locator('input[type="file"]');
    this.submitButton = page.getByRole('button', {
      name: /^(Submit Solution|Submitting\.\.\.)$/,
    });
    this.toast = page.locator('.p-toast-message').last();
  }

  async goto(eventId: string, levelId?: number): Promise<void> {
    // Do not deep-link directly to ?tab=submissions. EventDetailsComponent can
    // redirect that URL to overview while event/registration requests race.
    await openParticipantEventTab(this.page, eventId, 'Submissions', 'submissions');

    if (levelId !== undefined) {
      // The submissions component selects the first level and writes subtab to
      // the URL after its level request completes. These tests create one level.
      await expect(this.page).toHaveURL(
        new RegExp(`tab=submissions.*subtab=${levelId}|subtab=${levelId}.*tab=submissions`),
        { timeout: 20_000 },
      );
    }

    await expect(this.downloadResourcesButton).toBeVisible({ timeout: 20_000 });
  }

  async downloadResources(): Promise<import('@playwright/test').Download> {
    const downloadPromise = this.page.waitForEvent('download', { timeout: 20_000 });
    await this.downloadResourcesButton.click();
    return downloadPromise;
  }

  async submitSolution(sourceZipPath: string, outputJsonPath: string): Promise<void> {
    // The native file inputs are intentionally display:none in upload-area.scss.
    // Playwright can still set files on hidden <input type="file"> controls.
    await expect(this.sourceUploadInput).toHaveCount(1);
    await expect(this.outputUploadInput).toHaveCount(1);

    await this.sourceUploadInput.setInputFiles(sourceZipPath);
    await this.outputUploadInput.setInputFiles(outputJsonPath);

    await expect(this.submitButton).toBeEnabled();
    await this.submitButton.click();
    await expect(this.toast).toContainText('Submission Successful', { timeout: 20_000 });
  }
}
