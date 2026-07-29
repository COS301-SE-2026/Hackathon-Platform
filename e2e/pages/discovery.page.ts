import { expect, Locator, Page } from '@playwright/test'

export class DiscoveryPage {
    readonly page: Page;
    readonly welcomeBackText: Locator;
    readonly activeEventsSection: Locator;
    readonly openEventsSection: Locator;
    readonly searchInput: Locator;
    readonly activeEventsLoadError: Locator;
    readonly openEventsLoadError: Locator;
    readonly eventCards: Locator;
    readonly goToEventButton: Locator;
    readonly viewEventButton: Locator;
    readonly registerEventButton: Locator;
    readonly overviewTab: Locator;
    readonly rulesTab: Locator;
    readonly uploadSolutionTab: Locator;
    readonly submissionHistoryTab: Locator;
    readonly myTeamTab: Locator;
    readonly leaderboardTab: Locator;
    readonly eventContent: Locator;

    constructor(page: Page) {
        this.page = page;
        this.welcomeBackText = page.getByText('welcome back').first();
        this.activeEventsSection = page.getByText('Your Active Events', { exact: true });
        this.openEventsSection = page.getByRole('heading', { name: 'Open events', exact: true });
        this.searchInput = page.getByPlaceholder('search events');
        this.activeEventsLoadError = page.getByText('could not load your active events');
        this.openEventsLoadError = page.getByText('could not load open events');
        this.eventCards = page.locator('.event-card');
        this.goToEventButton = page.getByRole('button', { name: 'go to event' }).first();
        this.viewEventButton = page.getByRole('button', { name: 'view event' }).first();
        this.registerEventButton = page.getByRole('button', { name: 'Register', exact: true}).first();
        this.overviewTab = page.getByRole('button', { name: 'Overview', exact: true });
        this.rulesTab = page.getByRole('button', { name: 'Rules', exact: true });

        this.uploadSolutionTab = page.getByRole('button', { name: 'upload solution' });
        this.submissionHistoryTab = page.getByRole('button', { name: 'submission history' });
        this.myTeamTab = page.getByRole('button', { name: 'my team' });
        this.leaderboardTab = page.getByRole('button', { name: 'leaderboard' });
        this.eventContent = page.locator('.event-content');
    }

    async goto(): Promise<void> {
        await this.page.goto('/participant/home');
    }

    async gotoEventDetails(eventId: string): Promise<void> {
        await this.page.goto(`/participant/events/${eventId}`);
    }

    async expectBrowsePageVisible(): Promise<void> {
        await expect(this.welcomeBackText).toBeVisible();
        await expect(this.activeEventsSection).toBeVisible();
        await expect(this.openEventsSection).toBeVisible();
        await expect(this.searchInput).toBeVisible();
    }

    async expectEventsLoadedSuccessfully(): Promise<void> {
        if (await this.isVisible(this.activeEventsLoadError)) {
            throw new Error('The active event failed to be loaded');
        }

        if (await this.isVisible(this.openEventsLoadError)) {
            throw new Error('Open events have failed to be loaded');
        }
    }

    async openFirstEvent(): Promise<void> {
        if (await this.isVisible(this.goToEventButton)) {
            await this.goToEventButton.click();
        } else if (await this.isVisible(this.viewEventButton)) {
            await this.viewEventButton.click();
        } else {
            throw new Error('No buttons are visible');
        }

        await expect(this.page).toHaveURL('**/participant/events*');
    }

    async expectEventDetailsTabsVisible(): Promise<void> {
        await expect(this.overviewTab).toBeVisible();
        await expect(this.rulesTab).toBeVisible();
        await expect(this.uploadSolutionTab).toBeVisible();
        await expect(this.submissionHistoryTab).toBeVisible();
        await expect(this.myTeamTab).toBeVisible();
        await expect(this.leaderboardTab).toBeVisible();
    }

    async navigateToOverview(): Promise<void> {
        await this.overviewTab.click();
        await expect(this.overviewTab).toHaveClass('active');
    }

    async expectOverviewVisible(): Promise<void> {
        await expect(this.eventContent).toBeVisible();

        const overviewKeywords = this.eventContent.getByText('overview')
            .or(this.eventContent.getByText('description'))
            .or(this.eventContent.getByText('prize'))
            .or(this.eventContent.getByText('start date'))
            .or(this.eventContent.getByText('end date'));
        await expect(overviewKeywords.first()).toBeVisible();
    }

    async navigateToRules(): Promise<void> {
        await this.rulesTab.click();
        await expect(this.rulesTab).toHaveClass('active');
    }

    async expectRulesVisible(): Promise<void> {
        await expect(this.eventContent).toBeVisible();
        await expect(this.rulesTab).toHaveClass('active');
    }

    async navigateToMyTeam(): Promise<void> {
        await this.myTeamTab.click();
        await expect(this.myTeamTab).toHaveClass('active');
    }

    async hasRegisterButton(): Promise<boolean> {
        return this.isVisible(this.registerEventButton);
    }

    private async isVisible(locator: Locator): Promise<boolean> {
        try {
            await locator.waitFor({ state: 'visible', timeout: 2000 });
            return true;
        } catch {
            return false;
        }
    }
}