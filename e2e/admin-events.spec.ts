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

    test('navigates to manage, levels, and solver from event row',async ({page})=>{
        const events = new AdminEventsPage(page);
        const eventName = `E2E Nav Event ${Date.now()}`;
        

        await events.goto(hackathonId);
        await events.createEvent(eventName, '2025-01-15T09:00',48,4);
    
        await events.expectEventVisible(eventName);
        await events.navigateToManage(eventName);
        await expect(page).toHaveURL(/\/admin\/events\/.+\/manage$/);
        await page.goBack();

        await events.navigateToLevels(eventName);
        await expect(page).toHaveURL(/\/admin\/events\/.+\/levels$/);
        await page.goBack();

        await events.navigateToSolver(eventName);
        await expect(page).toHaveURL(/\/admin\/events\/.+\/solver$/);
        await page.goBack();


    });
;

    test('displays empty state when no events exist',async ({page})=>{
        const events = new AdminEventsPage(page);
        
        await events.goto(hackathonId);
        await events.waitForLoad();
        
        await expect(page.getByText('No events match your filter.')).toBeVisible();
    });


});