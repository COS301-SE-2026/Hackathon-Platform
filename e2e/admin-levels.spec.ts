import { test, expect } from '@playwright/test';
import * as path from 'path';
import { AdminLevelsPage } from './pages/admin-levels.page';
import { ApiClient, uniqueName } from './utils/api-client';

test.use({ storageState: path.resolve(__dirname, '../playwright/.auth/admin.json') });

test.describe('Admin: Levels', () => {
    let api: ApiClient;
    let hackathonId: string;
    let createdLevelIds: number[] = [];

    test.beforeAll(async () => {
        api = await ApiClient.loginAsAdmin();

    });

    test.afterAll(async () => {

        await api.dispose();
    });

    test.beforeEach(async () => {

        const seeded = await api.createHackathon(uniqueName('E2E Levels Parent'));
        hackathonId = seeded.hackathonId;

    });

    test.afterEach(async () => {
        await api.deleteLevels(hackathonId, createdLevelIds);
        createdLevelIds = [];
        await api.deleteHackathon(hackathonId);
    });

    test('creates a new level', async ({ page }) => {

        const levels = new AdminLevelsPage(page);
        const name = uniqueName('E2E Level');

        await levels.goto(hackathonId);
        await levels.addLevel(name, 1, 'Test description');

        await levels.expectLevelVisible(name);

        const levelId = await api.findLevelIdByName(hackathonId, name);
        if (levelId) createdLevelIds.push(levelId);
    });

    test('rejects creating a level with an empty name', async ({ page }) => {
        
        const levels = new AdminLevelsPage(page);

        await levels.goto(hackathonId);
        await levels.addLevelButton.click();
        await levels.levelNumberInput.fill('1');
        await levels.saveButton.click();

        await expect(levels.levelNameInput).toBeVisible();
        await expect(levels.modalError).toBeVisible();
        expect(await levels.getLevelCount()).toBe(0);

    });

    test('edits an existing level', async ({ page }) => {

        const seeded = await api.createLevel(hackathonId, uniqueName('E2E Edit Level'), 1);
        createdLevelIds.push(seeded.id);
        const editedName = uniqueName('E2E Edited Level');

        const levels = new AdminLevelsPage(page);
        await levels.goto(hackathonId);
        await levels.expectLevelVisible(seeded.name);

        await levels.editLevel(seeded.name, editedName, 1, 'Updated description');

        await levels.expectLevelVisible(editedName);
        await levels.expectLevelNotVisible(seeded.name);

    });

    test('rejects editing a level to an empty name', async ({ page }) => {

        const seeded = await api.createLevel(hackathonId, uniqueName('E2E Edit Invalid Level'), 1);
        createdLevelIds.push(seeded.id);

        const levels = new AdminLevelsPage(page);
        await levels.goto(hackathonId);
        await levels.expectLevelVisible(seeded.name);

        await levels.levelRow(seeded.name).getByRole('button', { name: 'Edit level' }).click();
        await levels.levelNameInput.fill('');
        await levels.saveButton.click();

        await expect(levels.levelNameInput).toBeVisible();
        await expect(levels.modalError).toBeVisible();
        await levels.expectLevelVisible(seeded.name);

    });

    test('deletes a level', async ({ page }) => {

        const seeded = await api.createLevel(hackathonId, uniqueName('E2E Delete Level'), 1);
        const levels = new AdminLevelsPage(page);
        await levels.goto(hackathonId);
        await levels.expectLevelVisible(seeded.name);

        await levels.deleteLevel(seeded.name);
        await levels.expectLevelNotVisible(seeded.name);

    });

    test('cancels deleting a level when the confirm dialog is dismissed', async ({ page }) => {

        const seeded = await api.createLevel(hackathonId, uniqueName('E2E Cancel Delete Level'), 1);
        createdLevelIds.push(seeded.id);

        const levels = new AdminLevelsPage(page);
        await levels.goto(hackathonId);
        await levels.expectLevelVisible(seeded.name);

        await levels.cancelDeleteLevel(seeded.name);
        await levels.expectLevelVisible(seeded.name);

    });

    test('reorders levels by dragging, and the new order persists after reload', async ({ page }) => {

        const first = await api.createLevel(hackathonId, uniqueName('E2E Order First'), 1);
        const second = await api.createLevel(hackathonId, uniqueName('E2E Order Second'), 2);
        createdLevelIds.push(first.id, second.id);

        const levels = new AdminLevelsPage(page);
        await levels.goto(hackathonId);

        expect(await levels.getOrderedLevelNames()).toEqual([first.name, second.name]);

        await levels.dragLevelTo(second.name, first.name);

        await expect(page.locator('.info-banner', { hasText: 'Saving new order...'})).toHaveCount(0, { timeout: 10000 });
        expect(await levels.getOrderedLevelNames()).toEqual([second.name, first.name]);

        await page.reload();
        await levels.waitForLoad();
        expect(await levels.getOrderedLevelNames()).toEqual([second.name, first.name]);

    });

    test('uploads a starter ZIP for a level directly from the levels list', async ({ page }) => {

        const seeded = await api.createLevel(hackathonId, uniqueName('E2E Starter Zip Level'), 1);
        createdLevelIds.push(seeded.id);
        const zipPath = path.resolve(__dirname, 'fixtures/sample-starter.zip');

        const levels = new AdminLevelsPage(page);
        await levels.goto(hackathonId);

        await levels.uploadStarterZip(seeded.name, zipPath);

        await expect(levels.levelRow(seeded.name).getByText('Uploading...')).toHaveCount(0, { timeout: 10000 });

        await levels.openManageFiles(seeded.name);
        await expect(page.locator('.file-row')).toHaveCount(1);
        await levels.closeManageFiles();

    });

    test('rejects a non-zip file for the starter upload', async ({ page }) => {

        const seeded = await api.createLevel(hackathonId, uniqueName('E2E Bad Starter File'), 1);
        createdLevelIds.push(seeded.id);
        const pdfPath = path.resolve(__dirname, 'fixtures/sample.pdf');

        const levels = new AdminLevelsPage(page);
        await levels.goto(hackathonId);

        await levels.uploadStarterZip(seeded.name, pdfPath);

        await expect(levels.errorBanner).toBeVisible();
        await levels.openManageFiles(seeded.name);
        await expect(page.locator('.file-row')).toHaveCount(0);
        await levels.closeManageFiles();


    });

    test('shows empty state when no levels exist', async ({ page }) => {

        const levels = new AdminLevelsPage(page);
        await levels.goto(hackathonId);

        await expect(page.getByText('No levels created yet')).toBeVisible();
    });

    test('navigates back to the hackathons list', async ({ page }) => {

        const levels = new AdminLevelsPage(page);
        await levels.goto(hackathonId);

        await levels.goBack();
        await expect(page).toHaveURL(/\/admin\/hackathons$/);


    });

});