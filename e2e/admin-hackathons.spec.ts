import { test, expect } from '@playwright/test';
import * as path from 'path';
import { AdminHackathonsPage } from './pages/admin-hackathons.page';
import { ApiClient, uniqueName } from './utils/api-client';

test.use({ storageState: path.resolve(__dirname, '../playwright/.auth/admin.json') });

test.describe('Admin: Hackathons CRUD', () => {
    let api: ApiClient;
    let createdHackathonIds: string[] = [];

    test.beforeAll(async () => {
        api = await ApiClient.loginAsAdmin();

    });

    test.afterAll(async () => {

        await api.dispose();
    });

    test.afterEach(async () => {
        await api.deleteHackathons(createdHackathonIds);
        createdHackathonIds = [];


    });

    test('creates a new hackathon', async ({ page }) => {

        const hackathons = new AdminHackathonsPage(page);
        const name = uniqueName('E2E Create Hackathon');

        await hackathons.goto();
        await hackathons.createHackathon(name, 'Created by Playwright e2e test');

        await hackathons.expectHackathonVisible(name);

        const hackathonId = await api.findHackathonIdByName(name);
        if (hackathonId) createdHackathonIds.push(hackathonId);
    });

    test('creates a hackathon with a problem statement PDF attached', async ({ page }) => {
        const hackathons = new AdminHackathonsPage(page);

        const name = uniqueName('E2E Hackathon PDF');
        const pdfPath = path.resolve(__dirname, 'fixtures/sample.pdf');

        await hackathons.goto();
        await hackathons.createHackathon(name, 'With problem statement', pdfPath);
        await hackathons.expectHackathonVisible(name);

        const hackathonId = await api.findHackathonIdByName(name);
        if (hackathonId) createdHackathonIds.push(hackathonId);


    });

    test('rejects creating a hackathon with an empty name', async ({ page }) => {
        
        const hackathons = new AdminHackathonsPage(page);

        await hackathons.goto();
        await hackathons.newHackathonButton.click();
        await hackathons.descriptionInput.fill('Missing a name');
        await hackathons.saveButton.click();

        await expect(hackathons.nameInput).toBeVisible();
        await expect(page.getByRole('button', { name: 'Save Changes' })).toHaveCount(0);

    });

    test('edits an existing hackathon', async ({ page }) => {

        const seeded = await api.createHackathon(uniqueName('E2E Edit Seed'), 'Original description');
        createdHackathonIds.push(seeded.hackathonId);

        const editedName = uniqueName('E2E Edited Hackathon');
        const hackathons = new AdminHackathonsPage(page);

        await hackathons.goto();
        await hackathons.expectHackathonVisible(seeded.name);
        await hackathons.editHackathon(seeded.name, editedName, 'Updated description');

        await hackathons.expectHackathonVisible(editedName);
        await hackathons.expectHackathonNotVisible(seeded.name);


    });

    test('rejects editing a hackathon to an empty name', async ({ page }) => {
        
        const seeded = await api.createHackathon(uniqueName('E2E Edit Invalid Seed'));
        createdHackathonIds.push(seeded.hackathonId);

        const hackathons = new AdminHackathonsPage(page);
        await hackathons.goto();
        await hackathons.expectHackathonVisible(seeded.name);

        await hackathons.card(seeded.name).getByRole('button', { name: 'Edit hackathon' }).click();
        await hackathons.nameInput.fill('');
        await hackathons.saveButton.click();

        await expect(hackathons.nameInput).toBeVisible();
        await hackathons.expectHackathonVisible(seeded.name);


    });

    test('deletes a hackathon', async ({ page }) => {

        const seeded = await api.createHackathon(uniqueName('E2E Delete Seed'));

        const hackathons = new AdminHackathonsPage(page);
        await hackathons.goto();
        await hackathons.expectHackathonVisible(seeded.name);

        await hackathons.deleteHackathon(seeded.name);
        await hackathons.expectHackathonNotVisible(seeded.name);

    });

    test('cancels deleting a hackathon when the confirm dialog is dismissed', async ({ page }) => {

        const seeded = await api.createHackathon(uniqueName('E2E Cancel Delete Seed'));
        createdHackathonIds.push(seeded.hackathonId);

        const hackathons = new AdminHackathonsPage(page);
        await hackathons.goto();
        await hackathons.expectHackathonVisible(seeded.name);

        await hackathons.cancelDeleteHackathon(seeded.name);
        await hackathons.expectHackathonVisible(seeded.name);

    });

    test('filters hackathons by search term', async ({ page }) => {

        const alpha = await api.createHackathon(uniqueName('E2E Alpha Search'));
        const beta = await api.createHackathon(uniqueName('E2E Beta Search'));
        createdHackathonIds.push(alpha.hackathonId, beta.hackathonId);

        const hackathons = new AdminHackathonsPage(page);
        await hackathons.goto();

        await hackathons.expectHackathonVisible(alpha.name);
        await hackathons.expectHackathonVisible(beta.name);

        await hackathons.search('Alpha');
        await hackathons.expectHackathonVisible(alpha.name);
        await hackathons.expectHackathonNotVisible(beta.name);

        await hackathons.search('');
        await hackathons.expectHackathonVisible(alpha.name);
        await hackathons.expectHackathonVisible(beta.name);

    });

    test('shows the no-match empty state for a search with no results', async ({ page }) => {

        const hackathons = new AdminHackathonsPage(page);
        await hackathons.goto();

        await hackathons.search(`nonexistent-${Date.now()}`);
        await hackathons.expectEmptyState(/No hackathons match your search/);


    });

    test('toggles between grid and list view, keeping the same hackathon visible', async ({ page }) => {

        const seeded = await api.createHackathon(uniqueName('E2E View Toggle'));
        createdHackathonIds.push(seeded.hackathonId);

        const hackathons = new AdminHackathonsPage(page);
        await hackathons.goto();

        await hackathons.setViewMode('list');
        await expect(page.locator('.hackathon-list')).toBeVisible();
        await hackathons.expectHackathonVisible(seeded.name);

        await hackathons.setViewMode('grid');
        await expect(page.locator('.hackathons-grid')).toBeVisible();
        await hackathons.expectHackathonVisible(seeded.name);


    });

});