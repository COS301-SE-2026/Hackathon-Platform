import { test as setup, expect } from '@playwright/test';
import * as dotenv from 'dotenv';
import * as fs from 'fs';
import * as path from 'path';
import { LoginPage } from './pages/login.page';

dotenv.config({ path: path.resolve(__dirname, '.env') });

const authDir = path.resolve(__dirname, 'playwright/.auth');
fs.mkdirSync(authDir, { recursive: true });

const adminFile = path.join(authDir, 'admin.json');
const participantFile = path.join(authDir, 'participant.json');
const participant2File = path.join(authDir, 'participant2.json');

function getCredentials(emailKey: string, passwordKey: string): { email: string; password: string } {
  const email = process.env[emailKey];
  const password = process.env[passwordKey];
  if (!email || !password) {
    throw new Error(`Missing ${emailKey} or ${passwordKey} in e2e/.env`);
  }
  return { email, password };
}

setup('login as admin', async ({ page }) => {
  const { email, password } = getCredentials('E2E_ADMIN_EMAIL', 'E2E_ADMIN_PASSWORD');
  const login = new LoginPage(page);
  await login.goto();
  await login.login(email, password);
  await expect(page).toHaveURL(/\/admin\/events(?:[/?#]|$)/, { timeout: 20_000 });
  await page.context().storageState({ path: adminFile });
});

setup('login as participant 1', async ({ page }) => {
  const { email, password } = getCredentials('E2E_PARTICIPANT_EMAIL', 'E2E_PARTICIPANT_PASSWORD');
  const login = new LoginPage(page);
  await login.goto();
  await login.login(email, password);
  await expect(page).toHaveURL(/\/participant\/home(?:[/?#]|$)/, { timeout: 20_000 });
  await page.context().storageState({ path: participantFile });
});

setup('login as participant 2', async ({ page }) => {
  const { email, password } = getCredentials('E2E_PARTICIPANT_2_EMAIL', 'E2E_PARTICIPANT_2_PASSWORD');
  const login = new LoginPage(page);
  await login.goto();
  await login.login(email, password);
  await expect(page).toHaveURL(/\/participant\/home(?:[/?#]|$)/, { timeout: 20_000 });
  await page.context().storageState({ path: participant2File });
});
