import {test, expect} from '@playwright/test'
import * as path from 'path';
import {AdminHackathonsPage} from './pages/admin-hackathons.page';

test.use ({storageState: path.resolve(__dirname,'../playwright/.auth/admin.json')});

test.describe('Admin: Hackathons', () => {
    test('creates a new hackathon',async ({page})=>{
        const hackathons = new AdminHackathonsPage(page);
        const name = `E2E hackathon ${Date.now()}`;

        await hackathons.goto();
        await hackathons.createHackathon(name, 'Created by Playwright e2e test');
        await hackathons.expectHackathonVisible(name);
    });

    test('creates a hackathon with problem statement',async ({page})=>{
        const hackathons = new AdminHackathonsPage(page);
        const name = `E2E hackathon PDF ${Date.now()}`;
        const pdfPath = path.resolve(__dirname,'fixtures/sample.pdf');

        await hackathons.goto();
        await hackathons.createHackathon(name, 'With problem statement',pdfPath);
        await hackathons.expectHackathonVisible(name);
    });

    test('edits an existing hackathon',async ({page})=>{
        const hackathons = new AdminHackathonsPage(page);
        const name = `E2E Edit Test ${Date.now()}`;
        const editedName = `${name} (edited)`;

        await hackathons.goto();
        await hackathons.createHackathon(name, 'Original description');
        await hackathons.expectHackathonVisible(name);

        await hackathons.editHackathon(name,editedName,'Updated description');
        await hackathons.expectHackathonVisible(editedName);
        await hackathons.expectHackathonNotVisible(name);

    });

    test('deletes a hackathon',async ({page})=>{
        const hackathons = new AdminHackathonsPage(page);
        const name = `E2E Delete Test ${Date.now()}`;

        await hackathons.goto();
        await hackathons.createHackathon(name);
        page.once('dialog',dialog => dialog.accept());
        await hackathons.deleteHackathon(name);
        await hackathons.expectHackathonNotVisible(name);
    });

    test('navigates to events, levels, and solver from hackathon card',async ({page})=>{
        const hackathons = new AdminHackathonsPage(page);
        const name = `E2E Nav hackathon ${Date.now()}`;

        await hackathons.goto();
        await hackathons.createHackathon(name);
        await hackathons.expectHackathonVisible(name);

        await hackathons.navigateToEvents(name);
        await expect(page).toHaveURL(/\/admin\/hackathons\/.+\/events$/);
        await page.goBack();

        await hackathons.navigateToCreateEvent(name);
        await expect(page).toHaveURL(/\/admin\/hackathons\/.+\/events\/create$/);
        await page.goBack();

        await hackathons.navigateToLevels(name);
        await expect(page).toHaveURL(/\/admin\/hackathons\/.+\/levels$/);
        await page.goBack();

        await hackathons.navigateToSolver(name);
        await expect(page).toHaveURL(/\/admin\/hackathons\/.+\/solver$/);
        await page.goBack();

        page.once('dialog',dialog => dialog.accept());
        await hackathons.deleteHackathon(name);

    });

    test('shows empty state when no hackathons exist',async ({page})=>{
        const hackathons = new AdminHackathonsPage(page);
        
        await hackathons.goto();
        const count = await hackathons.getHackathonCount();
        if(count > 0){
            const names = await hackathons.getHackathonNames();
            for (const name of names){
                page.once('dialog',dialog => dialog.accept());
                await hackathons.deleteHackathon(name);
            }
        }
        await expect(page.getByText('No hackathons created yet')).toBeVisible();
    });


});