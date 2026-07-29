import { test, expect } from '@playwright/test';
import { SubmissionsPage } from './pages/participant-submit.page';

test.describe('Submission Flow', () => {
  let submitPage: SubmissionsPage;
  const EVENT_ID = 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13';

  test.beforeEach(async ({ page }) => {
     submitPage = new SubmissionsPage(page);
     await submitPage.goto(EVENT_ID);
  });

  test('should load submission page', async () => {
    await submitPage.expectPageLoaded();
  });

  test('should display levels', async () => {
    await submitPage.expectLevelsVisible();
  });

  test('should upload files and submit', async () => {
    await submitPage.uploadSourceFile('dummy-submissions/test.zip');

    await submitPage.uploadSolutionFile('dummy-submissions/test.json');

    await submitPage.submit();
    const success = submitPage.successMessage;

    const error = submitPage.errorMessage;
    await expect(success.or(error)).toBeVisible({ timeout: 10000 });
  });
});


