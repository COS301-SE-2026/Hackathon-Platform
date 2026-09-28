import { expect, Locator, Page } from '@playwright/test'

export class SubmissionHistoryPage {
    readonly page: Page;
    readonly tableRows: Locator;
    readonly emptyState: Locator;

    constructor(page: Page) {
        this.page = page;
        this.tableRows = page.locator('.submission-history table tbody tr');
        this.emptyState = page.getByText('No Submission History');
    }

    async goToHistoryTab(eventId: string): Promise<void> {
        await this.page.goto(`/participant/events/${eventId}?tab=submission-history`)
    }

    rowForLevel(levelLable: string): Locator {
        return this.tableRows.filter({ hasText: levelLable });
    }

    dowloadLogButton(row: Locator): Locator {
        return row.getByRole('button', { name: /^Download Log|Downloading\.\.\.$/ });
    }
}