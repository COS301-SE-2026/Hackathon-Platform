import { test as setup, expect} from '@playwright/test';
import * as dotenv from 'dotenv';
import * as path from 'path';
import { LoginPage } from './pages/login.page';

dotenv.config({ path: path.resolve(__dirname, '.env')});
const adminFile = path.resolve(__dirname, '../playwright/.auth/admin.json');
const partFile = path.resolve(__dirname, '../playwright/.auth/participant.json');

setup('login as admin', async ({ page }) => {
    const email = process.env.E2E_ADMIN_EMAIL;
    const password = process.env.E2E_ADMIN_PASSWORD;
    if(!email || !password){
        throw new Error(" no email or password set");
    }

    const login = new LoginPage(page);
    await login.goto();
    await login.ligin(email, password);
    await expect(page).toHaveURL(/\/admin\/dashboard/);
    await page.context().storageState({ path: adminFile });
});

setup('login as user', async ({ page }) => {
    const email = process.env.E2E_PARTICIPANT_EMAIL;
    const password = process.env.E2E_PARTICIPANT_PASSWORD;
    if(!email || !password){
        throw new Error(" no email or password set");
    }

    const login = new LoginPage(page);
    await login.goto();
    await login.ligin(email, password);
    await expect(page).toHaveURL(/\/participant\/home/);
    await page.context().storageState({ path: partFile });
});