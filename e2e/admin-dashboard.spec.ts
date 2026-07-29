import {test, expect} from '@playwright/test'
import * as path from 'path';

import {AdminDashboardPage } from './pages/admin-dashboard.page';

test.use ({storageState: path.resolve(__dirname,'../playwright/.auth/admin.json')});

test.describe('Admin: Dashboard', () => {
    test('displays dashboard stats',async ({page}) => {
        const dashboard = new AdminDashboardPage(page);

        await dashboard.goto();
        await dashboard.expectStatsVisible();
        await dashboard.expectSubmissionsTableVisible();

        const activeEvents = await dashboard.getActiveEvents();
        expect(activeEvents).toMatch(/^\d+$/);

        const totalParticipants = await dashboard.getTotalParticipants();
        expect(totalParticipants).toMatch(/^\d+$/);

        const submissionsToday = await dashboard.getSubmissionsToday();
        expect(submissionsToday).toMatch(/^\d+$/);
    });

    test('navigate to hackathons from new event button',async ({page}) => {
        const dashboard = new AdminDashboardPage(page);
        await dashboard.goto();

        await dashboard.clickNewEvent();
        await expect(page).toHaveURL(/\/admin\/hackathons/);


    }); 

    test('displays recent events list',async ({page}) => {
        const dashboard = new AdminDashboardPage(page);
        await dashboard.goto();

        const hasEvents = await page.locator('.event-item').count() >0 ;
        const hasEmptyState = await page.getByText('No events have been loaded yet').isVisible();

         await expect(hasEvents || hasEmptyState).toBeTruthy();
    });

   
    
});    
