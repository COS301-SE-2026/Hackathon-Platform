import {Page, Locator,expect} from '@playwright/test'

export class ForumPage{
    constructor(private readonly page: Page, private readonly eventId: string, private readonly eventName: string, private readonly isAdmin = true){}
        
    
    
async goto(): Promise<void>{
    if (this.isAdmin){
    await this.page.goto(`/admin/events`);
    
    
    await this.page.waitForLoadState('networkidle');

    const eventRow = this.page.locator('.event-card', {hasText: this.eventName}).first();
    await eventRow.waitFor({state: 'visible', timeout: 5000});
    await eventRow.getByText('View event').click();

    await this.page.waitForSelector('.modal-panel',{timeout: 10_000});
    await this.page.locator('.modal-tabs .tab-btn',{hasText: 'Forum'}).click();
    await this.page.waitForSelector('.forum-page',{timeout: 10_000});


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
    const textarea = card.locator('.reply-compose textarea');
    const postBtn = card.getByRole('button', {name: /post reply/i});

    for (let attempt = 0; attempt<3; attempt++){
        await textarea.fill(body);
        try{
            await expect(postBtn).toBeEnabled({timeout: 3_000});
            await postBtn.click({timeout: 3_000});
            return;
        }catch{
            //retry
        }
    }
    await textarea.fill(body);
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