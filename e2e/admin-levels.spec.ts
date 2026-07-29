import {test, expect} from '@playwright/test'
import * as path from 'path';
import {AdminHackathonsPage} from './pages/admin-hackathons.page';
import {AdminLevelsPage } from './pages/admin-levels.page';

test.use ({storageState: path.resolve(__dirname,'../playwright/.auth/admin.json')});

test.describe('Admin: Levels', () => {
    let hackathonId: string;

    test.beforeEach(async ({page})=>{
        const hackathons = new AdminHackathonsPage(page);
        const name = `E2E Levels Test ${Date.now()}`;

        await hackathons.goto();
        await hackathons.createHackathon(name);
        await hackathons.expectHackathonVisible(name);
        await hackathons.navigateToLevels(name);
        hackathonId = page.url().match(/\/admin\/hackathons\/([^\/]+)\/levels/)?.[1] || '';
    });

    test('creates a new level',async ({page})=>{
        const levels = new AdminLevelsPage(page);
        const levelName = `E2E Level ${Date.now()}`;
        

        await levels.goto(hackathonId);
        await levels.addLevel(levelName,  1,'Test description');
        await levels.expectLevelVisible(levelName);
    });

    test('edits an existing level',async ({page})=>{
        const levels = new AdminLevelsPage(page);
        const name = `E2E edit level ${Date.now()}`;
        const editedName = `${name} (edited)`;

        await levels.goto(hackathonId);
        await levels.addLevel(name,1);
        await levels.editLevel(name,editedName,2);
        await levels.expectLevelVisible(editedName);
        await levels.expectLevelNotVisible(name);
    });

     test('deletes a level',async ({page})=>{
        const levels = new AdminLevelsPage(page);
        const name = `E2E Delete Level ${Date.now()}`;

        await levels.goto(hackathonId);
        await levels.addLevel(name,1);
        await levels.expectLevelVisible(name);

        page.once('dialog',dialog => dialog.accept());
        await levels.deleteLevel(name);
        await levels.expectLevelNotVisible(name);
    });

    test('navigates back to hackathon',async ({page})=>{
        const levels = new AdminLevelsPage(page);
            

        await levels.goto(hackathonId);
         await levels.goBack();
        await expect(page).toHaveURL(new RegExp(`/admin/hackathons/${hackathonId}`));
       


    });
;

    test('show empty state when no levels exist',async ({page})=>{
        const levels = new AdminLevelsPage(page);
        
        await levels.goto(hackathonId);
            
        await expect(page.getByText('No levels created yet')).toBeVisible();
    });


});