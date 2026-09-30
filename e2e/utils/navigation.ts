import { expect, Page } from '@playwright/test';

/**
 * Open a protected participant event tab through the visible tab navigation.
 *
 * The EventDetailsComponent loads event data and registration status in parallel.
 * Deep-linking directly to a protected tab such as ?tab=submissions can race those
 * requests and the component may redirect back to overview before event data has
 * finished loading. Starting on Overview and clicking the rendered tab waits for
 * the component itself to confirm that the user is registered and the tab is
 * currently available.
 */
export async function openParticipantEventTab(
  page: Page,
  eventId: string,
  label: string,
  tab: string,
): Promise<void> {
  await page.goto(`/participant/events/${eventId}?tab=overview`, {
    waitUntil: 'domcontentloaded',
  });

  await expect(page).toHaveURL(
    new RegExp(`/participant/events/${eventId}.*tab=overview`),
  );

  const eventTabs = page.locator('section.event-tabs nav.tabs');
  await expect(eventTabs).toBeVisible();

  const target = eventTabs.getByRole('link', { name: label, exact: true });
  await expect(target).toBeVisible({ timeout: 20_000 });
  await target.click();

  await expect(page).toHaveURL(
    new RegExp(`/participant/events/${eventId}.*tab=${tab}`),
    { timeout: 20_000 },
  );
}
