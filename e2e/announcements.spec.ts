import { test, expect } from './fixtures/auth.fixture';
import {AnnouncementsPage} from './pages/announcements.page'

test.describe('admin > Announcements', () => {
    test('admin creates an announcement and the participant sees it', async ({
        adminPage,
        participantPage,
        eventId,
        uniqueSuffix,
    }) => {
        const title = `Kickoff ${uniqueSuffix}`;
        const body = 'Welcome to the event!';

        const admin = new AnnouncementsPage(adminPage,eventId);
        await admin.goto();
        await admin.openCreateModal();
        await admin.fillForm(title, body,'Important');
        await admin.submit();
        await admin.expectCardVisible(title);

        await expect(
            admin.cardByTitle(title).locator('.severity-badge'),

        ).toContainText(/important/i);

        const participant = new AnnouncementsPage(participantPage, eventId);
        await participant.gotoParticipant();
        await participant.expectCardVisible(title);
        await expect(
            participant.cardByTitle(title).locator('.severity-badge'),
        ).toContainText(/important/i);
    });

    test('validation: empty title blocks submit and shows error', async({
        adminPage,
        eventId,

    }) => {
        const admin = new AnnouncementsPage(adminPage, eventId);
        await admin.goto();
        await admin.openCreateModal();
        await admin.submit();
        await expect(
            adminPage.locator('.modal-card .error-banner')
        ).toContainText(/title is required/i,

        );
    });

    test('validation: empty message blocks submit and shows error', async({
        adminPage,
        eventId,

    }) => {
        const admin = new AnnouncementsPage(adminPage, eventId);
        await admin.goto();
        await admin.openCreateModal();
        await adminPage.locator('#announcementTitle').fill('Has a title');
            await admin.submit();
        await expect(
            adminPage.locator('.modal-card .error-banner')
        ).toContainText(/message is required/i,

        );        
    });

    test('severity selection is preserved on save', async ({ adminPage,eventId, uniqueSuffix}) =>{
        const title = `Urgent ${uniqueSuffix}`;
        const admin =new AnnouncementsPage(adminPage, eventId);

        await admin.goto();
        await admin.openCreateModal();
        await admin.fillForm(title,'Something broke','Urgent');
        await admin.submit();
       
        await expect(
            admin.cardByTitle(title).locator('.severity-badge'),

        ).toContainText(/urgent/i);

    });

    test('participant list is empty for a fresh event', async({
        participantPage,
        eventId,

    }) =>{
     const participant = new AnnouncementsPage(participantPage, eventId);
        await participant.gotoParticipant();
        await expect(
            participantPage.locator('.empty-state')).toContainText(/no announcements yet/i,
                
            );
    });


});
    

