import { expect, test } from '@playwright/test';
import * as path from 'path';
import { LeaderboardPage } from './pages/leaderboard.page';
import { SubmissionHistoryPage } from './pages/submission-history.page';
import { SubmissionPage } from './pages/submissions.page';
import { ADMIN_STATE, PARTICIPANT_STATE, apiForState } from './utils/api';
import {
  createEvent,
  createHackathon,
  createLevel,
  createTeam,
  deleteHackathonQuietly,
  freezeLeaderboard,
  registerForEvent,
  uniqueName,
  uploadLevelResource,
  uploadSolver,
  waitForScoredSubmission,
} from './utils/test-data';

test.describe.serial('Participant: submissions, scoring and leaderboard', () => {
  let hackathonId: string;
  let levelId: number;
  let eventId: string;
  let teamName: string;
  let teamId: string;

  const sourceZip = path.resolve(__dirname, 'fixtures/submission-source.zip');
  const output10 = path.resolve(__dirname, 'fixtures/output-10.json');
  const output99 = path.resolve(__dirname, 'fixtures/output-99.json');

  test.beforeAll(async () => {
    const adminApi = await apiForState(ADMIN_STATE);
    const participantApi = await apiForState(PARTICIPANT_STATE);

    const hackathon = await createHackathon(adminApi, uniqueName('E2E-Scoring-Hackathon'));
    hackathonId = hackathon.hackathonId;

    const level = await createLevel(adminApi, hackathonId);
    levelId = level.id;

    await uploadSolver(adminApi, hackathonId);
    await uploadLevelResource(adminApi, hackathonId, levelId);

    const event = await createEvent(adminApi, hackathonId, {
      name: uniqueName('E2E-Scoring-Event'),
      visibility: 'PUBLIC',
      active: true,
    });
    eventId = event.eventId;

    await registerForEvent(participantApi, eventId);
    teamName = uniqueName('E2E-Scoring-Team');
    const team = await createTeam(participantApi, eventId, teamName);
    teamId = team.teamId;

    await adminApi.dispose();
    await participantApi.dispose();
  });

  test.afterAll(async () => {
    if (!hackathonId) return;
    const adminApi = await apiForState(ADMIN_STATE);
    await deleteHackathonQuietly(adminApi, hackathonId);
    await adminApi.dispose();
  });

  test('downloads level files, submits, shows history, and updates leaderboard', async ({ page }) => {
    const submissions = new SubmissionPage(page);
    const history = new SubmissionHistoryPage(page);
    const leaderboard = new LeaderboardPage(page);

    await submissions.goto(eventId, levelId);

    const download = await submissions.downloadResources();
    expect(download.suggestedFilename()).toBe('level-resource.txt');

    await submissions.submitSolution(sourceZip, output10);

    const participantApi = await apiForState(PARTICIPANT_STATE);
    await waitForScoredSubmission(participantApi, teamId, 10);
    await participantApi.dispose();

    await history.goto(eventId);
    await history.expectScored('Level 1', 10);

    await leaderboard.goto(eventId);
    await leaderboard.expectTeamScore(teamName, 10);
  });

  test('leaderboard freezes at the configured cutoff, including live SSE refreshes', async ({ page }) => {
    const adminApi = await apiForState(ADMIN_STATE);
    const participantApi = await apiForState(PARTICIPANT_STATE);
    const leaderboard = new LeaderboardPage(page);

    const sseConnected = page.waitForRequest(
      request => request.url().includes(`/api/scoring/events/${eventId}/leaderboard/update`),
      { timeout: 15_000 },
    );

    await leaderboard.goto(eventId);
    await sseConnected;
    await leaderboard.expectTeamScore(teamName, 10);

    await freezeLeaderboard(adminApi, eventId);
    await page.waitForTimeout(750);

    const frozenRefresh = page.waitForResponse(
      response =>
        response.request().method() === 'GET' &&
        response.url().includes(`/api/scoring/events/${eventId}/leaderboard`),
      { timeout: 60_000 },
    );

    const submitPage = await page.context().newPage();
    const submissions = new SubmissionPage(submitPage);
    await submissions.goto(eventId, levelId);
    await submissions.submitSolution(sourceZip, output99);
    await waitForScoredSubmission(participantApi, teamId, 99);

    const refreshResponse = await frozenRefresh;
    expect(refreshResponse.ok()).toBeTruthy();

    const history = new SubmissionHistoryPage(submitPage);
    await history.goto(eventId);
    await history.expectSubmissionCountAtLeast(2);
    await expect(history.tableRows.first()).toContainText('Completed');
    await expect(history.tableRows.first()).toContainText(/\b99(?:\.0+)?\b/);

    await leaderboard.expectTeamScore(teamName, 10);
    await expect(leaderboard.rowForTeam(teamName)).not.toContainText('99.00 pts');

    await submitPage.close();
    await participantApi.dispose();
    await adminApi.dispose();
  });
});
