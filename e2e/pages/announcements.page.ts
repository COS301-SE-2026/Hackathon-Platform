import {Page, Locator,expect} from '@playwright/test'

export class AnnouncementsPage{
    constructor(private readonly page: Page, private readonly eventId:string , private readonly eventName:string ){}

async goto(): Promise<void>{
    await this.page.goto(`/admin/events`);
    await this.page.waitForSelector('.event-item, [class*="event"]', {
    timeout: 10_000});

    await this.page.getByText(this.eventName, {exact: true}).first().click();

    await this.page.waitForSelector('.modal-panel');
    await this.page.getByRole('button', {name:/^\s*Announcements\s*$/i }).click();
    await this.page.waitForSelector('.announcement-page', {timeout: 10_000});
}

async gotoParticipant(){
    await this.page.goto(`/participant/events/${this.eventId}/announcements`);
    await this.page.waitForSelector('.announcement-page');
}

get cards():Locator{
    return this.page.locator('.announcement-card');
}

cardByTitle(title:string): Locator{
    return this.cards.filter({has: this.page.locator('.announcement-title',{hasText:title})});
}
async openCreateModal(){
    await this.page.getByRole('button', {name: /new announcement/i}).click();
    await expect(this.page.locator('.modal-card')).toBeVisible();
}

async fillForm(title: string, message: string, severity: 'Info'| 'Important' | 'Urgent' = 'Info'){
    await this.page.locator('#announcementTitle').fill(title);
    await this.page.locator('#announcementMessage').fill(message);
    await this.page.locator('.severity-option',{hasText: severity}).click();
}

async submit(){
    await this.page.getByRole('button',{name:/post announcement/i}).click();
}

async expectCardVisible(title: string){
    await expect(this.cardByTitle(title)).toBeVisible();
}
}