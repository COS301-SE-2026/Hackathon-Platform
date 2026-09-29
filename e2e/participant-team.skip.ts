import { expect, test } from '@playwright/test';
import { MyTeamPage } from './pages/my-team.page';
import {
  ADMIN_STATE,
  PARTICIPANT_2_STATE,
  PARTICIPANT_STATE,
  apiForState,
} from './utils/api';
import {
  createEvent,
  createHackathon,
  deleteHackathonQuietly,
  getMe,
  registerForEvent,
  uniqueName,
} from './utils/test-data';

test.describe.serial('Participant: My Team', () => {
  let hackathonId: string;

  test.beforeAll(async () => {
    const adminApi = await apiForState(ADMIN_STATE);
    const hackathon = await createHackathon(adminApi, uniqueName('E2E-Team-Hackathon'));
    hackathonId = hackathon.hackathonId;
    await adminApi.dispose();
  });

  test.afterAll(async () => {
    if (!hackathonId) return;
    const adminApi = await apiForState(ADMIN_STATE);
    await deleteHackathonQuietly(adminApi, hackathonId);
    await adminApi.dispose();
  });

  test('create, request-to-join, approve, copy join code, and leave', async ({ browser }) => {
    const adminApi = await apiForState(ADMIN_STATE);
    const p1Api = await apiForState(PARTICIPANT_STATE);
    const p2Api = await apiForState(PARTICIPANT_2_STATE);

    const event = await createEvent(adminApi, hackathonId, { active: true });
    await registerForEvent(p1Api, event.eventId);
    await registerForEvent(p2Api, event.eventId);
    const participant2 = await getMe(p2Api);
    const participant2Name = `${participant2.firstName} ${participant2.lastName}`;

    const p1Context = await browser.newContext({ storageState: PARTICIPANT_STATE });
    const p2Context = await browser.newContext({ storageState: PARTICIPANT_2_STATE });
    await p1Context.grantPermissions(['clipboard-read', 'clipboard-write'], {
      origin: process.env.E2E_BASE_URL ?? 'http://localhost:4200',
    });

    const p1Page = await p1Context.newPage();
    const p2Page = await p2Context.newPage();
    const p1Team = new MyTeamPage(p1Page);
    const p2Team = new MyTeamPage(p2Page);
    const teamName = uniqueName('E2E-Team');

    await p1Team.goto(event.eventId);
    await p1Team.expectNoTeamState();
    await p1Team.createTeam(teamName);

    const joinCode = await p1Team.getJoinCode();
    expect(joinCode).not.toBe('');
    await p1Team.copyJoinCode();
    await expect.poll(() => p1Page.evaluate(() => navigator.clipboard.readText())).toBe(joinCode);

    await p2Team.goto(event.eventId);
    await p2Team.expectNoTeamState();
    await p2Team.requestToJoin(joinCode);

    await p1Team.goto(event.eventId);
    await p1Team.approveRequest(participant2Name);
    await expect(p1Team.memberRow(participant2Name)).toBeVisible();

    await p2Team.goto(event.eventId);
    await p2Team.expectTeamName(teamName);
    await p2Team.leaveTeam();

    await p1Context.close();
    await p2Context.close();
    await adminApi.dispose();
    await p1Api.dispose();
    await p2Api.dispose();
  });

  test('rejects a pending join request', async ({ browser }) => {
    const adminApi = await apiForState(ADMIN_STATE);
    const p1Api = await apiForState(PARTICIPANT_STATE);
    const p2Api = await apiForState(PARTICIPANT_2_STATE);

    const event = await createEvent(adminApi, hackathonId, { active: true });
    await registerForEvent(p1Api, event.eventId);
    await registerForEvent(p2Api, event.eventId);
    const participant2 = await getMe(p2Api);
    const participant2Name = `${participant2.firstName} ${participant2.lastName}`;

    const p1Context = await browser.newContext({ storageState: PARTICIPANT_STATE });
    const p2Context = await browser.newContext({ storageState: PARTICIPANT_2_STATE });
    const p1Team = new MyTeamPage(await p1Context.newPage());
    const p2Team = new MyTeamPage(await p2Context.newPage());

    await p1Team.goto(event.eventId);
    await p1Team.createTeam(uniqueName('E2E-Reject-Team'));
    const joinCode = await p1Team.getJoinCode();

    await p2Team.goto(event.eventId);
    await p2Team.requestToJoin(joinCode);

    await p1Team.goto(event.eventId);
    await p1Team.rejectRequest(participant2Name);

    await p2Team.goto(event.eventId);
    await p2Team.expectNoTeamState();

    await p1Context.close();
    await p2Context.close();
    await adminApi.dispose();
    await p1Api.dispose();
    await p2Api.dispose();
  });
});
