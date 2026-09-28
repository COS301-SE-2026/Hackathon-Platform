import { test, expect } from '@playwright/test';
import * as path from 'path';
import { AdminEventsPage } from './pages/admin-events.page';
import { ApiClient, uniqueName } from './utils/api-client';

test.use({ storageState: path.resolve(__dirname, '../playwright/.auth/admin.json') });


test.describe('Admin: Event creation', () => {

    let api: ApiClient;
    let hackathonId: string;

    test.beforeAll(async () => {
        api = await ApiClient.loginAsAdmin();


    });

    test.afterAll(async () => {
        await api.dispose();
    });

    test.beforeEach(async () => {

        const seeded = await api.createHackathon(uniqueName('E2E Event Parent'));
        hackathonId = seeded.hackathonId;

    });

    test.afterEach(async () => {
        
        await api.deleteHackathon(hackathonId);
    });

    test('creates a minimal public event', async ({ page }) => {

        const events = new AdminEventsPage(page);
        const name = uniqueName('E2E Public Event');

        await events.goto(hackathonId);
        await events.createEvent({
            name,
            startDate: '2027-01-15',
            startTime: '09:00',
            duration: 48,
            teamSizeLimit: 4,
            description: 'Minimal public event',

        });

        await expect(page).toHaveURL(new RegExp(`/admin/hackathons/${hackathonId}/events$`));
        await events.expectEventVisible(name);
        expect((await events.getEventVisibility(name)).toUpperCase()).toContain('PUBLIC');

        
    });



});