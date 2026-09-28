import { expect, Page, Locator } from '@playwright/test';

export class MyTeamPage {
    readonly page: Page;
    readonly createTeamButton: Locator;
    readonly requestToJoinButton: Locator;
    readonly teamNameInput: Locator;
    readonly joinCodeInput: Locator;
    readonly dialogPrimaryButton: Locator;
    readonly dialogCancelButton: Locator;
    readonly dialogError: Locator;
    readonly leaveTeamButton: Locator;
    readonly confirmLeaveButton: Locator;
    readonly copyJoinCodeButton: Locator;
    readonly joinCodeValue: Locator;
    readonly memberRows: Locator;
    readonly pendingRequestRows: Locator;
    readonly toast: Locator;

    constructor(page: Page) {
        this.page = page;
        this.createTeamButton = page.getByRole('button', { name: 'Create Team', exact: true });
        this.requestToJoinButton = page.getByRole('button', { name: 'Request To Join', exact: true });
        this.teamNameInput = page.getByLabel('Team Name');
        this.joinCodeInput = page.getByLabel('Join Code');
        this.dialogPrimaryButton = page.locator('app-model').getByRole('button', { name: /^Create Team|Send$/ });
        this.dialogCancelButton = page.locator('app-model').getByRole('button', { name: 'Cancel' });
        this.dialogError = page.locator('.team-dialog-error');
        this.leaveTeamButton = page.getByRole('button', { name: 'Leave Team', exact: true }).first();
        this.confirmLeaveButton = page.locator('app-model').filter({ hasText: 'Leave Team' }).getByRole('button', { name: 'Leave Team ', exact: true });
        this.copyJoinCodeButton = page.getByRole('button', { name: /^Cop(y|ying\.\.\.)$/ });
        this.joinCodeValue = page.locator('.join-code');
        this.memberRows = page.locator('.member-row');
        this.pendingRequestRows = page.locator('.request-row');
        this.toast = page.locator('.p-toast-message');
    }

    async goToTeamTab(eventId: string): Promise<void> {
        await this.page.goto(`/participant/events/${eventId}?tab=team`);
    }

    async expectNoTeamState(): Promise<void> {
        await expect(this.page.getByText("You're not part of a team")).toBeVisible();
    }

    async createTeam(teamName: string): Promise<void> {
        await this.createTeamButton.click();
        await this.teamNameInput.fill(teamName);
        await this.dialogPrimaryButton.click();
    }

    async requestToJoinByCode(joinCode: string): Promise<void> {
        await this.requestToJoinButton.click();
        await this.joinCodeInput.fill(joinCode);
        await this.dialogPrimaryButton.click();
    }

    async expectTeamNameVisible(teamName: string): Promise<void> {
        await expect(this.page.locator('.team-name h1')).toHaveText(teamName);
    }

    memberRow(name: string): Locator {
        return this.memberRows.filter({ hasText: name });
    }

    pendingRequestRow(name: string): Locator {
        return this.pendingRequestRows.filter({ hasText: name });
    }

    async approveRequest(name: string): Promise<void> {
        await this.pendingRequestRow(name).getByRole('button', { name: 'Approve request' }).click();
    }

    async rejectRequest(name: string): Promise<void> {
        await this.pendingRequestRow(name).getByRole('button', { name: 'Reject request' }).click();
    }

    async leaveTeam(): Promise<void> {
        await this.leaveTeamButton.click();
        await this.confirmLeaveButton.click();
    }

    async copyJoinCode(): Promise<void> {
        await this.copyJoinCodeButton.click();
    }

    async getJoinCode(): Promise<string> {
        return (await this.joinCodeValue.textContent())?.trim() ?? '';
    }
}