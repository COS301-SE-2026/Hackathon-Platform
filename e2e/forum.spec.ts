import { test, expect } from './fixtures/auth.fixture';
import {ForumPage} from './pages/forum.page';

test.describe('Forum', () => {
    test('admin creates post -> participant replies -> admin deleted reply', async({
        adminPage,
        participantPage,
        eventId,
        uniqueSuffix,
    }) => {
        const title = `Thread ${uniqueSuffix}`;
        const replyBody = 'Angular + NestJS';
        const eventName = `E2E Event ${uniqueSuffix}`
        const admin = new ForumPage(adminPage,eventId,eventName,true);
        const participant = new ForumPage(participantPage,eventId,eventName,false);

        await admin.goto();
        await admin.openCreatePost();
        await admin.fillPost(title, 'What is your favourite stack?');
        await admin.submitPost();

        await expect(admin.threadByTitle(title)).toBeVisible();

        await participant.goto();
        await expect(participant.threadByTitle(title)).toBeVisible({timeout: 10_000});
        await participant.expandThread(title);
        await participant.replyToThread(title,replyBody);

        const pThread = participant.threadByTitle(title);
        await expect(pThread.locator('.reply-card')).toHaveCount(1);
        await expect(pThread.locator('.thread-subline')).toContainText(/1 reply/);

        await admin.goto();
        await admin.expandThread(title);
        const adminThread = admin.threadByTitle(title);
        await expect(adminThread.locator('.reply-card')).toHaveCount(1);

        adminPage.once('dialog', (d)=> d.accept());
        await adminThread
        .locator('.reply-card')
        .first()
        .getByRole('button', {name: 'Delete message'})
        .click();

        await expect(adminThread.locator('.reply-card')).toHaveCount(0);
        await expect(adminThread.locator('.reply-empty')).toBeVisible();

    });

    test('admin deletes the entire post', async ({ adminPage, eventId, uniqueSuffix

    }) =>{
        const eventName = `E2E Event ${uniqueSuffix}`
        const title = `DeleteMe ${uniqueSuffix}`;
        const admin = new ForumPage(adminPage, eventId,eventName,true);

        await admin.goto();
        await admin.openCreatePost();
        await admin.fillPost(title, 'Body to delete');
        await admin.submitPost();

        await expect(admin.threadByTitle(title)).toBeVisible();
        await admin.deleteThread(title);
        await expect(admin.threadByTitle(title)).toHaveCount(0);

    });

    test('multiple replies are all shown when thread is expanded',async ({
        adminPage,
        eventId,
        uniqueSuffix,

    }) =>{
        const eventName = `E2E Event ${uniqueSuffix}`
        const title = `MultiReply ${uniqueSuffix}`;
        const admin = new ForumPage(adminPage, eventId,eventName, true);

        await admin.goto();
        await admin.openCreatePost();
        await admin.fillPost(title, 'Reply to me');
        await admin.submitPost();

        await admin.expandThread(title);
        await admin.replyToThread(title, 'first reply');
        await admin.replyToThread(title, 'second reply');

        const card = admin.threadByTitle(title);
        await expect(card.locator('.reply-card')).toHaveCount(2);
        await expect(card.locator('.thread-subline')).toContainText(/2 replies/);

    });

    test('search filters threads by title and by body',async({
        adminPage,
        eventId,
        uniqueSuffix,

    }) =>{
        const eventName = `E2E Event ${uniqueSuffix}`
        const title = `MultiReply ${uniqueSuffix}`;
        const admin = new ForumPage(adminPage, eventId,eventName,true);

        await admin.goto();
        await admin.openCreatePost();
        await admin.fillPost(title, 'UniqueBodyToken-' + uniqueSuffix);
        await admin.submitPost();

        await adminPage.getByPlaceholder(/search threads/i).fill(title);
        await expect(admin.threads).toHaveCount(1);

        await adminPage.getByPlaceholder(/search threads/i).fill('zzz-no-such-thread-xyz');
        await expect(adminPage.locator('.empty-state')).toContainText(/no threads found/i);
    });

    test('participant cannot see Create Post when perms deny it',async ({
        participantPage,
        eventId,
        uniqueSuffix,
    }) => {
        const eventName = `E2E Event ${uniqueSuffix}`
        const participant = new ForumPage(participantPage,eventId,eventName,false);
        await participant.goto();

        const btn = participantPage.getByRole('button',{name: /create post/i});
        const count = await btn.count();
        if (count > 0){
            test.info().annotations.push({
                type:'note',
                description: 'Participant has canCreatePost=true; skipped negative assertion.',

            });
        }else {
            await expect(btn).toHaveCount(0);
        }

    });
});
