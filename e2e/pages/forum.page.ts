import {Page, Locator,expect} from '@playwright/test'

export class ForumPage{
    constructor(private readonly page: Page, private readonly eventId: string, private readonly eventName: string, private readonly isAdmin = true){}
        
    
    
async goto(): Promise<void>{
    if (this.isAdmin){
    await this.page.goto(`/admin/events`);
    
    
    await this.page.waitForLoadState('networkidle');
    const url = this.page.url();
    const bodyText = await this.page.locator('body').innerText();
    console.log('=== E2E DEBUG (forum) ===');
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
    console.log('===END E2E DEBUG (forum)===')


    const eventLink = this.page.getByText(this.eventName, { exact: false}).first();
    await eventLink.waitFor({ state: 'visible',timeout:5000});
    await eventLink.click();

    await this.page.waitForSelector('.modal-panel', {timeout: 10_000});
    await this.page.locator('.modal-tabs .tab-btn', {hasText: 'Forum'}).click();
    await this.page.waitForSelector('.forum-page', {timeout: 10_000});

    await this.page.getByText(this.eventName, {exact: true}).first().click();


    } else {
        await this.page.goto(`/participant/events/${this.eventId}/forum`);
        await this.page.waitForSelector('.forum-page', {timeout: 10_000});
    }
} 

get threads():Locator {
    return this.page.locator('.thread-card');
}

threadByTitle(title:string):Locator{
    return this.threads.filter({has: this.page.locator('.thread-title',{hasText: title})});
}
async openCreatePost(){
    await this.page.getByRole('button',{name: /create post/i}).click();
    await expect(this.page.locator('.modal-card')).toBeVisible();
}

async fillPost(title: string, body: string){
    await this.page.locator('#postTitle').fill(title);
    await this.page.locator('#postBody').fill(body);
}

async submitPost(){
    await this.page
    .locator('.modal-card')
    .getByRole('button',{name: /^create post$/i})
    .click();
}

async expandThread(title: string){
    const card = this.threadByTitle(title);
    await card.getByRole('button', {name: /view thread/i}).click();
    await expect(card.locator('.thread-panel')).toBeVisible();
}

async replyToThread(title: string, body: string){
    const card = this.threadByTitle(title);
    await card.locator('.reply-compose textarea').fill(body);
    const postBtn = card.getByRole('button', {name: /post reply/i});
    await expect(postBtn).toBeEnabled({ timeout: 5_000});
    await postBtn.click();

}

async deleteThread(title: string){
    const card = this.threadByTitle(title);
    const panel = card.locator('.thread-panel');

    if (!(await panel.isVisible().catch(()=> false))){
        await card.getByRole('button',{name: /view thread/i}).click();
        await expect(panel).toBeVisible();
    }
    
    this.page.once('dialog', (d) => d.accept());
    await card.locator('.message-card').first()
    .getByRole('button', {name: 'Delete message'})
    .click();
}

}