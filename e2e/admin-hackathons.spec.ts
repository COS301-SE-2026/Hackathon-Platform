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

});