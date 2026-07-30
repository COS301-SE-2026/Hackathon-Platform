import { expect, Locator, Page } from '@playwright/test'

export class TeamPage {
    readonly page: Page;
    readonly myTeamTab: Locator;
    readonly myTeamContent: Locator;
    readonly createTeamButton: Locator;
    readonly joinTeamButton: Locator;
    readonly requestToJoinButton: Locator;
    readonly teamNameInputByPlaceholder: Locator;
    readonly createTeamSubmitButton: Locator;   
    readonly joinCodeInputByLabel: Locator;
    readonly joinTeamSubmitButton: Locator;

    constructor(page: Page) {
        this.page = page;

        this.myTeamTab = page.getByRole('button', { name: 'my team' });
        this.myTeamContent = page.locator('.event-content');
        this.createTeamButton = page.getByRole('button', { name: 'create team' }).first();
        this.requestToJoinButton = page.getByRole('button', { name: 'request to join' }).first();
        this.joinTeamButton = page.getByRole('button', { name: 'Join Team', exact: true }).first();
        this.teamNameInputByPlaceholder = page.getByPlaceholder('team name').first();
        this.createTeamSubmitButton = page.getByRole('button', { name: 'create'}).first();
        this.joinCodeInputByLabel = page.getByLabel('team code').first();
        this.joinTeamSubmitButton = page.getByRole('button', { name: 'join'}).first();
    }

    async goToMyTeam(): Promise<void> {
        await this.myTeamTab.click();
        await expect(this.myTeamTab).toHaveClass('active');
        await expect(this.myTeamContent).toBeVisible();
    }

    async expectMyTeamTabVisible(): Promise<void> {
        await expect(this.myTeamContent.getByText(`you're currently not part of a team`).first()).toBeVisible();
    }

    async startCreateTeamFlow(): Promise<boolean> {
        await this.goToMyTeam();

        if (!(await this.isVisible(this.createTeamButton))) {
            return false;
        }

        await this.createTeamButton.click();
        return true;
    }

    async expectCreateTeamFormVisible(): Promise<void> {
        const teamNameInput = this.teamNameInputByPlaceholder;
        await expect(teamNameInput).toBeVisible();
    }

    async createTeam(teamName: string): Promise<boolean> {
        const started = await this.startCreateTeamFlow();

        if (!started) {
            return false;
        }

        const teamNameInput = this.teamNameInputByPlaceholder;

        await teamNameInput.fill(teamName);
        await this.createTeamSubmitButton.click();

        return true;
    }

    async startJoinTeamFlow(): Promise<boolean> {
        await this.goToMyTeam();

        if(await this.isVisible(this.joinTeamButton)) {
            await this.joinTeamButton.click();
            return true;
        }

        if (await this.isVisible(this.requestToJoinButton)) {
            await this.requestToJoinButton.click();
            return true;
        }

        return false;
    }

    async expectJoinTeamFlowVisible(): Promise<void> {
        await expect(this.page.getByText('join team').first()).toBeVisible();
    }

    async joinTeam(joinCode: string): Promise<boolean> {
        const started = await this.startJoinTeamFlow();

        if (!started) {
            return false;
        }

        const joinCodeInput = await this.getVisibleJoinCodeInput();

        if (joinCodeInput) {
            await joinCodeInput.fill(joinCode);
        }

        if (await this.isVisible(this.joinTeamSubmitButton)) {
            await this.joinTeamSubmitButton.click();
        }

        return true;
    }

    private async getVisibleJoinCodeInput(): Promise<Locator | null> {
        if (await this.isVisible(this.joinCodeInputByLabel)) {
            return this.joinCodeInputByLabel;
        }

        return null;
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