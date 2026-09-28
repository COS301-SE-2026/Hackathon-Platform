import {Page, Locator,expect} from '@playwright/test'

export class AdminTeamPage{
    constructor(private readonly page: Page, private readonly eventId:string , private readonly eventName:string){}

async goto(): Promise<void>{
    await this.page.goto(`/admin/events`);
    
    await this.page.waitForLoadState('networkidle');
    const url = this.page.url();
    const bodyText = await this.page.locator('body').innerText();
    console.log('=== E2E DEBUG ===');
    console.log('URL after navigation:',url);
    console.log('Body Text (first 3000 charts):');
    console.log(bodyText.slice(0,3000));
    console.log('---event-rows---');
    console.log('count of .event-item',await this.page.locator('.event-item').count());
    console.log('count of .event-card',await this.page.locator('.event-card').count());
    console.log('count of .event-row',await this.page.locator('.event-row').count());
    console.log('count of [class*="event"]:',await this.page.locator('[class*="event"]').count());
    console.log('count of a[href*="events/"]:',await this.page.locator('a[href*="events/"]').count());
    console.log('count of getByText(eventName):',await this.page.getByText(this.eventName, {exact: false}).count());
    console.log('===END E2E DEBUG===')


    const eventLink = this.page.getByText(this.eventName, { exact: false}).first();
    await eventLink.waitFor({ state: 'visible',timeout:5000});
    await eventLink.click();

    await this.page.waitForSelector('.modal-panel', {timeout: 10_000});
    await this.page.locator('.modal-tabs .tab-btn', {hasText: 'Teams'}).click();
    await this.page.waitForSelector('.teams-page', {timeout: 10_000});
}

get teamCards(): Locator{
    return this.page.locator('.team-card');
}

get searchInput(): Locator{
    return this.page.getByPlaceholder(/search teams/i);
}
get createTeamBtn(): Locator{
    return this.page.getByRole('button',{ name: /create team/i});
}

async fillTeamName(name: string){
    await this.page.locator('#newTeamName').fill(name);
}

async addPendingMember(name: string, email = ''){
    await this.page.locator('#newMemberName').fill(name);
    if (email) await this.page.locator('#newMemberEmail').fill(email);
    await this.page.getByRole('button',{name: /add participant/i}).click();
}

async confirmCreateTeam() {
    await this.page
    .getByRole('dialog')
    .getByRole('button',{name: /^create team$/i})
    .click();
    await expect(this.page.getByRole('dialog')).toBeHidden();
}

teamCardByName(name: string): Locator{
    return this.teamCards.filter({has: this.page.locator('.team-title',{hasText: name}) });
}

async expandTeam(name: string){
    const card = this.teamCardByName(name);
    await card.getByRole('button',{name: /view team/i }).click();
    await expect(card.locator('.team-panel')).toBeVisible();
}

async addMemberToTeam(teamName: string, memberName: string, email = ''){
    const card = this.teamCardByName(teamName);
    const inputs = card.locator('.member-add-row input');
    await inputs.nth(0).fill(memberName);
    if (email) await inputs.nth(1).fill(email);
    await card.getByRole('button',{name: /add member/i }).click();
}

async removeMemberFromTeam(teamName: string, memberName: string){
    const card = this.teamCardByName(teamName);
    const memberCard = card.locator('.member-card', {hasText: memberName});
    await memberCard.locator('.member-delete-btn').click();
}

async deleteTeam(teamName: string){
    const card = this.teamCardByName(teamName);
    await card.locator('.btn-delete-team').click();
}
}