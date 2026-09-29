import { expect, Locator, Page } from '@playwright/test';
import { openParticipantEventTab } from '../utils/navigation';

export class MyTeamPage {
  readonly page: Page;
  readonly createTeamButton: Locator;
  readonly requestToJoinButton: Locator;
  readonly teamNameInput: Locator;
  readonly joinCodeInput: Locator;
  readonly leaveTeamButton: Locator;
  readonly copyJoinCodeButton: Locator;
  readonly joinCodeValue: Locator;
  readonly memberRows: Locator;
  readonly pendingRequestRows: Locator;

  constructor(page: Page) {
    this.page = page;
    this.createTeamButton = page
      .locator('.empty-state-actions')
      .getByRole('button', { name: 'Create Team', exact: true });
    this.requestToJoinButton = page
      .locator('.empty-state-actions')
      .getByRole('button', { name: 'Request To Join', exact: true });
    this.teamNameInput = page.locator('input#teamName');
    this.joinCodeInput = page.locator('input#teamJoinCode');
    this.leaveTeamButton = page
      .locator('.team-actions')
      .getByRole('button', { name: 'Leave Team', exact: true });
    this.copyJoinCodeButton = page
      .locator('.invite-card-content')
      .getByRole('button', { name: /^(Copy|Copying\.\.\.)$/ });
    this.joinCodeValue = page.locator('.join-code');
    this.memberRows = page.locator('.member-row');
    this.pendingRequestRows = page.locator('.request-row');
  }

  async goto(eventId: string): Promise<void> {
    await openParticipantEventTab(this.page, eventId, 'Team', 'team');
  }

  async expectNoTeamState(): Promise<void> {
    await expect(this.page.getByText("You're not part of a team", { exact: true })).toBeVisible();
  }

  private modal(title: string): Locator {
    // <app-modal> is a custom-element host with no box. The visible element is
    // the .modal-overlay rendered inside it.
    return this.page.locator('.modal-overlay').filter({ hasText: title });
  }

  async createTeam(teamName: string): Promise<void> {
    await expect(this.createTeamButton).toBeVisible();
    await this.createTeamButton.click();

    const dialog = this.modal('Create Team');
    await expect(dialog).toBeVisible();
    await expect(dialog.getByRole('heading', { name: 'Create Team', exact: true })).toBeVisible();

    await expect(this.teamNameInput).toBeVisible();
    await this.teamNameInput.fill(teamName);
    await dialog.getByRole('button', { name: 'Create Team', exact: true }).click();

    await expect(dialog).toBeHidden();
    await this.expectTeamName(teamName);
  }

  async requestToJoin(joinCode: string): Promise<void> {
    await expect(this.requestToJoinButton).toBeVisible();
    await this.requestToJoinButton.click();

    const dialog = this.modal('Request to Join Team');
    await expect(dialog).toBeVisible();
    await expect(
      dialog.getByRole('heading', { name: 'Request to Join Team', exact: true }),
    ).toBeVisible();

    await expect(this.joinCodeInput).toBeVisible();
    await this.joinCodeInput.fill(joinCode);
    await dialog.getByRole('button', { name: 'Send', exact: true }).click();
    await expect(dialog).toBeHidden();
  }

  async expectTeamName(teamName: string): Promise<void> {
    await expect(this.page.locator('.team-name h1')).toHaveText(teamName);
  }

  memberRow(name: string): Locator {
    return this.memberRows.filter({ hasText: name });
  }

  pendingRequestRow(name: string): Locator {
    return this.pendingRequestRows.filter({ hasText: name });
  }

  async approveRequest(name: string): Promise<void> {
    const row = this.pendingRequestRow(name);
    await expect(row).toBeVisible();
    await row.getByRole('button', { name: 'Approve request', exact: true }).click();
    await expect(row).toBeHidden();
    await expect(this.memberRow(name)).toBeVisible();
  }

  async rejectRequest(name: string): Promise<void> {
    const row = this.pendingRequestRow(name);
    await expect(row).toBeVisible();
    await row.getByRole('button', { name: 'Reject request', exact: true }).click();
    await expect(row).toBeHidden();
  }

  async leaveTeam(): Promise<void> {
    await expect(this.leaveTeamButton).toBeVisible();
    await this.leaveTeamButton.click();

    const dialog = this.modal('Leave Team');
    await expect(dialog).toBeVisible();
    await expect(dialog.getByRole('heading', { name: 'Leave Team', exact: true })).toBeVisible();
    await dialog.getByRole('button', { name: 'Leave Team', exact: true }).click();

    await expect(dialog).toBeHidden();
    await this.expectNoTeamState();
  }

  async copyJoinCode(): Promise<void> {
    await expect(this.copyJoinCodeButton).toBeVisible();
    await this.copyJoinCodeButton.click();
  }

  async getJoinCode(): Promise<string> {
    await expect(this.joinCodeValue).toBeVisible();
    return (await this.joinCodeValue.textContent())?.trim() ?? '';
  }
}
