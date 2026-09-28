import { expect, Page, Locator } from '@playwright/test';

export class SubmissionPage {
    readonly page: Page;
    readonly downloadResourcesButton: Locator;
    readonly sourceUploadInput: Locator;
    readonly outputUploadInput: Locator;
    readonly submitButton: Locator;
    readonly toast: Locator;

    constructor(page: Page) {
        this.page = page;
        this.downloadResourcesButton = page.getByRole('button', { name: 'Download Resources' });
        this.sourceUploadInput = page.locator('.upload-time', { hasText: 'Solution Archive' }).locator('input[type="file"]');
        this.outputUploadInput = page.locator('.upload-time', { hasText: 'Output File' }).locator('input[type="file"]');
        this.submitButton = page.getByRole('button', { name: /^ Submit Solution|Submitting\.\.\.$/ });
        this.toast = page.locator('.p-toast-message');
    }

    async gotoSubmissionsTab(eventId: string, levelId?: number): Promise<void> {
        const subTab = levelId ? `$subtab=${levelId}` : '';
        await this.page.goto(`/participant/events/${eventId}?tab=submissions${subTab}`);
    }

    async downloadResources(): Promise<import('@playwright/test').Download> {
        const [download] = await Promise.all([
            this.page.waitForEvent('download'),
            this.downloadResourcesButton.click(),
        ]);
        return download;
    }

    async submitSolution(sourceZipPath: string, outputJsonPath: string): Promise<void> {
        await this.sourceUploadInput.setInputFiles(sourceZipPath);
        await this.outputUploadInput.setInputFiles(outputJsonPath);
        await expect(this.submitButton).toBeEnabled();
        await this.submitButton.click();
    }
}
