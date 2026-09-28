import {Page, Locator,expect} from '@playwright/test'

export class ForumPage{
    constructor(private readonly page: Page, private readonly eventId: string, private readonly eventName: string, private readonly isAdmin = true){}
        
    
    
async goto(): Promise<void>{
    if (this.isAdmin){
    await this.page.goto(`/admin/events`);
    await this.page.waitForSelector('.event-item, [class*="event"]', {
    timeout: 10_000});

    await this.page.getByText(this.eventName, {exact: true}).first().click();

    await this.page.waitForSelector('.modal-panel');
    await this.page.getByRole('button', {name:/^\s*Forum\s*$/i }).click();
    await this.page.waitForSelector('.forum-page', {timeout: 10_000});
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