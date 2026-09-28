import {Page, Locator,expect} from '@playwright/test'

export class AnnouncementsPage{
    constructor(private readonly page: Page, private readonly eventId:string , private readonly eventName:string ){}

async goto(): Promise<void>{
       await this.page.goto(`/admin/events`);
    
    await this.page.waitForLoadState('networkidle');
    const url = this.page.url();
    const bodyText = await this.page.locator('body').innerText();
    console.log('=== E2E DEBUG (announcements) ===');
    console.log('URL after navigation:',url);
    console.log('Body Text (first 3000 chars):');
    console.log(bodyText.slice(0,3000));
    console.log('---event-rows---');
    console.log('count of .event-item',await this.page.locator('.event-item').count());
    console.log('count of .event-card',await this.page.locator('.event-card').count());
    console.log('count of .event-row',await this.page.locator('.event-row').count());
    console.log('count of [class*="event"]:',await this.page.locator('[class*="event"]').count());
    console.log('count of a[href*="events/"]:',await this.page.locator('a[href*="events/"]').count());
    console.log('count of getByText(eventName):',await this.page.getByText(this.eventName, {exact: false}).count());
    console.log('===END E2E DEBUG (announcements)===')


    const eventLink = this.page.getByText(this.eventName, { exact: false}).first();
    await eventLink.waitFor({ state: 'visible',timeout:5000});
    await eventLink.click();

    await this.page.waitForSelector('.modal-panel', {timeout: 10_000});
    await this.page.locator('.modal-tabs .tab-btn', {hasText: 'Announcements'}).click();
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