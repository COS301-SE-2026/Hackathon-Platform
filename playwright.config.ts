import { defineConfig, devices } from '@playwright/test';
import * as dotenv from 'dotenv';
import * as path from 'path';

dotenv.config({ path: path.resolve(__dirname, 'e2e/.env') });

const baseURL = process.env.E2E_BASE_URL ?? 'http://localhost:4200';

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  workers: 1,
  timeout: 90_000,

  expect: {
    timeout: 15_000,
  },

  reporter: [
    ['list'],
    ['html', { outputFolder: 'playwright-report', open: 'never' }],
  ],

  use: {
    baseURL,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },

  projects: [
    {
      name: 'setup',
      testMatch: /.*\.setup\.ts/,
      use: {
        ...devices['Desktop Chrome'],
      },
    },
    {
      name: 'chromium',
      dependencies: ['setup'],
      testMatch: /participant-.*\.spec\.ts/,
      use: {
        ...devices['Desktop Chrome'],
        storageState: path.resolve(
          __dirname,
          'e2e/playwright/.auth/participant.json',
        ),
      },
    },
  ],

  webServer: {
    command: 'npm --prefix frontend start',
    url: baseURL,
    reuseExistingServer: true,
    timeout: 120_000,
  },
});
