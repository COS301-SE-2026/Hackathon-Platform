import { expect, test } from '@playwright/test';
import { EventDetailsPage } from './pages/event-details.page';
import { HomePage } from './pages/home.page';
import { ADMIN_STATE, PARTICIPANT_STATE, apiForState } from './utils/api';
import { createEvent, createHackathon, deleteHackathonQuietly } from './utils/test-data';

test.describe.serial('Participant: browse and register', () => {
  let hackathonId: string;
  let publicEvent: { eventId: string; name: string };
  let privateEvent: { eventId: string; name: string };
  const privateKey = 'E2E-SECRET-123';

  test.beforeAll(async () => {
    const adminApi = await apiForState(ADMIN_STATE);
    const hackathon = await createHackathon(adminApi);
    hackathonId = hackathon.hackathonId;

    publicEvent = await createEvent(adminApi, hackathonId, {
      name: `Public Browse ${Date.now()}`,
      visibility: 'PUBLIC',
    });

    privateEvent = await createEvent(adminApi, hackathonId, {
      name: `Private Browse ${Date.now()}`,
      visibility: 'PRIVATE',
      registrationKey: privateKey,
    });

    await adminApi.dispose();
  });

  test.afterAll(async () => {
    if (!hackathonId) return;
    const adminApi = await apiForState(ADMIN_STATE);
    await deleteHackathonQuietly(adminApi, hackathonId);
    await adminApi.dispose();
  });

  test.skip('searches upcoming events', async ({ page }) => {
    const home = new HomePage(page);
    await home.goto();
    await home.openUpcoming();

    await home.search(publicEvent.name);
    await expect(home.eventCard(publicEvent.name)).toBeVisible();
    await expect(home.eventCard(privateEvent.name)).toHaveCount(0);

    await home.openEvent(publicEvent.name);
    await expect(page).toHaveURL(new RegExp(`/participant/events/${publicEvent.eventId}`));
  });

  test('registers for a public event and blocks duplicate registration', async ({ page }) => {
    const details = new EventDetailsPage(page);
    await details.goto(publicEvent.eventId);
    await details.registerPublic();
    await details.expectRegistered();
    await expect(details.toast).toContainText('Registration Successful');

    const participantApi = await apiForState(PARTICIPANT_STATE);
    const duplicate = await participantApi.post(`/api/events/${publicEvent.eventId}/registered`, { data: {} });
    expect(duplicate.ok()).toBeFalsy();
    expect((await duplicate.text()).toLowerCase()).toContain('already registered');
    await participantApi.dispose();
  });

  test('private registration rejects a bad key and accepts the correct key', async ({ page }) => {
    const details = new EventDetailsPage(page);
    await details.goto(privateEvent.eventId);

    await details.registerPrivate('WRONG-KEY');
    await expect(details.toast).toContainText('Registration Failed');
    await expect(details.registrationDialog).toBeVisible();

    await details.submitPrivateKey(privateKey);
    await details.expectRegistered();
    await expect(details.toast).toContainText('Registration Successful');
  });
});
