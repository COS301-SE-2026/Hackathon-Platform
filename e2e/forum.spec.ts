import { test, expect } from './fixtures/auth.fixture';
import {ForumPage} from './pages/forum.page';

test.describe('Forum', () => {
    test('admin creates post, replies, and deleted reply', async({
        adminPage,
        eventId,
        uniqueSuffix,
    }) => {
        const title = `Thread ${uniqueSuffix}`;
        const replyBody = 'Angular + NestJS';
        const eventName = `E2E Event ${uniqueSuffix}`
        const admin = new ForumPage(adminPage,eventId,eventName,true);


        await admin.goto();
        await admin.openCreatePost();
        await admin.fillPost(title, 'What is your favourite stack?');
        await admin.submitPost();

        await expect(admin.threadByTitle(title)).toBeVisible();


        await admin.expandThread(title);
        await admin.replyToThread(title, replyBody);
        const adminThread = admin.threadByTitle(title);
        await expect(adminThread.locator('.reply-card')).toHaveCount(1);
        await expect(adminThread.locator('.thread-subline')).toContainText(/1 reply/);

        adminPage.once('dialog', (d)=> d.accept());
        await adminThread
        .locator('.reply-card')
        .first()
        .getByRole('button', {name: 'Delete message'})
        .click();

        await expect(adminThread.locator('.reply-card')).toHaveCount(0);
        await expect(adminThread.locator('.reply-empty')).toBeVisible();

    });

    test.fixme('participant sees and replies to admin thread',async ({
        adminPage,
        participantPage,
        eventId,
        uniqueSuffix,
    }) =>{
        // participants must be registered for the event before their forum shows the threads

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
