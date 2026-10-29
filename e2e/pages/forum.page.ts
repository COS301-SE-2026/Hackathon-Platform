import {Page, Locator,expect} from '@playwright/test'

export class ForumPage{
    constructor(private readonly page: Page, private readonly eventId: string, private readonly eventName: string, private readonly isAdmin = true){}
        
    
    
    async goto(): Promise<void> {
    if (this.isAdmin) {
        await this.page.goto('http://localhost:4200/admin/events');

        const eventRow = this.page.locator('.event-card', {
            hasText: this.eventName
        }).first();

        await expect(eventRow).toBeVisible();

        await eventRow.getByText('View event').click();

        await expect(this.page.locator('.modal-panel')).toBeVisible();

        await this.page.locator('.modal-tabs .tab-btn', {
            hasText: 'Forum'
        }).click();

        await expect(this.page.locator('.forum-page')).toBeVisible();

    } else {
        await this.page.goto(
            `http://localhost:4200/participant/events/${this.eventId}?tab=forum`
        );

        await expect(this.page.locator('.forum-page')).toBeVisible();
    }
}
get threads():Locator {
    return this.page.locator('.thread-card');
}

threadByTitle(title:string):Locator{
    return this.threads.filter({has: this.page.locator('.thread-title',{hasText: title})});
}

async openCreatePost(): Promise<void> {
    await this.page.getByRole('button', {
        name: 'Create Post',
        exact: true
    }).click();

    const modal = this.page.locator('app-modal').filter({
        has: this.page.locator('h2', {
            hasText: 'Create Post'
        })
    });

    await expect(modal.locator('dialog.modal')).toBeVisible();
}

async fillPost(title: string, body: string): Promise<void> {
    await this.page.locator('input#postTitle').fill(title);
    await this.page.locator('textarea#postBody').fill(body);
}

async submitPost(): Promise<void> {
    const modal = this.page.locator('app-modal').filter({
        has: this.page.locator('h2', { hasText: 'Create Post' })
    });

    await modal.getByRole('button', {
        name: 'Create Post',
        exact: true
    }).click();

    await expect(modal).toHaveCount(0);
}

async expandThread(title: string){
    const card = this.threadByTitle(title);
    await card.getByRole('button', {name: /view thread/i}).click();
    await expect(card.locator('.thread-panel')).toBeVisible();
}

async replyToThread(title: string, body: string): Promise<void> {
    const card = this.threadByTitle(title);

    const textarea = card.locator('.reply-compose textarea');

    const postBtn = card.getByRole('button', {
        name: 'Post Reply'
    });

    const replies = card.locator('.reply-card');
    const previousCount = await replies.count();

    await textarea.fill(body);

    await expect(postBtn).toBeEnabled();

    await postBtn.click();

    await expect(replies).toHaveCount(previousCount + 1);
}

async deleteThread(title: string): Promise<void> {
    const card = this.threadByTitle(title);
    const panel = card.locator('.thread-panel');

    if (!(await panel.isVisible())) {
        await card.getByRole('button', {
            name: /view thread/i
        }).click();

        await expect(panel).toBeVisible();
    }

    await card
        .locator('.message-card')
        .first()
        .getByRole('button', {
            name: 'Delete message'
        })
        .click();

    const modal = this.page.locator('app-modal').filter({
        has: this.page.locator('h2', {
            hasText: 'Delete Post?'
        })
    });

    await expect(modal.locator('dialog.modal')).toBeVisible();

    await modal.getByRole('button', {
        name: 'Delete',
        exact: true
    }).click();

    await expect(card).toHaveCount(0);
}

}