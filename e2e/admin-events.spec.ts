import {test, expect} from '@playwright/test'
import * as path from 'path';
import {AdminHackathonsPage} from './pages/admin-hackathons.page';
import {AdminEventsPage } from './pages/admin-events.page';

test.use ({storageState: path.resolve(__dirname,'../playwright/.auth/admin.json')});

test.describe('Admin: Events', () => {
    let hackathonId: string;

    test.beforeEach(async ({page})=>{
        const hackathons = new AdminHackathonsPage(page);
        const name = `E2E Event Test ${Date.now()}`;

        await hackathons.goto();
        await hackathons.createHackathon(name);
        await hackathons.expectHackathonVisible(name);
        await hackathons.navigateToEvents(name);
        hackathonId = page.url().match(/\/admin\/hackathons\/([^\/]+)\/events/)?.[1] || '';
    });

    test('creates a new event for a hackathon',async ({page})=>{
        const events = new AdminEventsPage(page);
        const eventName = `E2E Event ${Date.now()}`;
        const startDate = '2025-01-15T09:00';

        await events.goto(hackathonId);
        await events.createEvent(eventName, startDate, 48, 4, 'Test event description');
        await events.expectEventVisible(eventName);
    });

    test('filters event by search',async ({page})=>{
        const events = new AdminEventsPage(page);
        const event1 = `E2E Alpha ${Date.now()}`;
        const event2 = `E2E Beta ${Date.now()}`;

        await events.goto(hackathonId);
        await events.createEvent(event1, '2025-01-15T09:00',48,4);
        await events.createEvent(event2, '2025-01-15T09:00',24,3);

        await events.searchEvents('Alpha');
        await events.expectEventVisible(event1);
        await events.expectEventNotVisible(event2);

        await events.searchEvents('');
        await events.expectEventVisible(event1);
        await events.expectEventVisible(event2);



    });



});