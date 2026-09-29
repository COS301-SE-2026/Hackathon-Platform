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

    test('creates a private event with a registration key', async ({ page }) => {

        const events = new AdminEventsPage(page);
        const name = uniqueName('E2E Private Event');

        await events.goto(hackathonId);
        await events.createEvent({
            name,
            startDate: '2027-01-15',
            startTime: '09:00',
            duration: 24,
            teamSizeLimit: 3,
            visibility: 'PRIVATE',
            registrationKey: 'letmein-e2e',
        });

        await expect(page).toHaveURL(new RegExp(`/admin/hackathons/${hackathonId}/events$`));
        await events.expectEventVisible(name);
        expect((await events.getEventVisibility(name)).toUpperCase()).toContain('PRIVATE');

    });

    test('rejects a private event with no registration key', async ({ page }) => {

        const events = new AdminEventsPage(page);
        const name = uniqueName('E2E Private Missing Key');

        await events.goto(hackathonId);
        await events.gotoCreateForm();
        await events.fillCreateForm({
            name,
            startDate: '2027-01-15',
            startTime: '09:00',
            duration: 24,
            teamSizeLimit: 3,
            visibility: 'PRIVATE',

        });

        await events.submitCreateForm();

        await expect(page).toHaveURL(/\/events\/create$/);
        await expect(events.createErrorMessage).toBeVisible();

        await events.goto(hackathonId);
        await events.expectEventNotVisible(name);

    });

    test('rejects an event with missing required fields', async ({ page }) => {

        const events = new AdminEventsPage(page);

        await events.goto(hackathonId);
        await events.gotoCreateForm();
        await events.teamSizeInput.fill('4');
        await events.durationInput.fill('24');
        await events.submitCreateForm();

        await expect(page).toHaveURL(/\/events\/create$/);
        await expect(events.createErrorMessage).toBeVisible();

    })

    test('cancels event creation without creating the event', async ({ page }) => {

        const events = new AdminEventsPage(page);
        const name = uniqueName('E2E Canceled Before Create');

        await events.goto(hackathonId);
        await events.gotoCreateForm();
        await events.fillCreateForm({
            name,
            startDate: '2027-01-15',
            startTime: '09:00',
            duration: 12,
            teamSizeLimit: 2,
        });

        await events.cancelCreateForm();

        await expect(page).toHaveURL(new RegExp(`/admin/hackathons/${hackathonId}/events$`));
        await events.expectEventNotVisible(name);

    });

    test('searches events by name', async ({ page }) => {

        const events = new AdminEventsPage(page);
        const alpha = uniqueName('E2E Alpha Event');
        const beta = uniqueName('E2E Beta Event');

        await api.createEvent(hackathonId, { name: alpha });
        await api.createEvent(hackathonId, { name: beta });

        await events.goto(hackathonId);
        await events.searchEvents('Alpha');
        await events.expectEventVisible(alpha);
        await events.expectEventNotVisible(beta);

        await events.searchEvents('');
        await events.expectEventVisible(alpha);
        await events.expectEventVisible(beta);

    });

    test('shows the no-match empty state for a search with no results', async ({ page }) => {

        await api.createEvent(hackathonId, { name: uniqueName('E2E Search Seed Event')});

        const events = new AdminEventsPage(page);
        await events.goto(hackathonId);

        await events.searchEvents(`nonexistent-${Date.now()}`);
        await events.expectEmptyState(/No events match your search/);
    });



});