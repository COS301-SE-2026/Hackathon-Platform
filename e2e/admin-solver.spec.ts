import {test, expect} from '@playwright/test'
import * as path from 'path';
import {AdminHackathonsPage} from './pages/admin-hackathons.page';
import {AdminSolverPage } from './pages/admin-solver.page';

test.use ({storageState: path.resolve(__dirname,'../playwright/.auth/admin.json')});

test.describe('Admin: Solver', () => {
    let hackathonId: string;

    test.beforeEach(async ({page})=>{
        const hackathons = new AdminHackathonsPage(page);
        const name = `E2E Solver Test ${Date.now()}`;

        await hackathons.goto();
        await hackathons.createHackathon(name);
        await hackathons.expectHackathonVisible(name);
        await hackathons.navigateToSolver(name);
        hackathonId = page.url().match(/\/admin\/hackathons\/([^\/]+)\/solver/)?.[1] || '';


});

    test('upload a solver version',async ({page})=>{
        const solver = new AdminSolverPage(page);
        const filePath = path.resolve(__dirname, 'fixtures/solver.py');
        
        await solver.goto(hackathonId);
        await solver.uploadSolverVersion(filePath,'Initial version');
    });

    test('display version history',async ({page}) =>{
        const solver = new AdminSolverPage(page);

        await solver.goto(hackathonId);
        const hasVersions = await solver.getVersionCount() > 0;
        const hasEmptyState = await page.getByText('No solver versions uploaded yet').isVisible();
        await expect(hasVersions || hasEmptyState).toBeTruthy();
});

    test('navigates back to hackathon',async ({page}) =>{
        const solver = new AdminSolverPage(page);
        await solver.goto(hackathonId);
        await solver.goBack();
        await expect(page).toHaveURL(new RegExp(`/admin/hackathons/${hackathonId}`));
    });

});            