import { APIRequestContext, expect } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';

export function uniqueName(prefix: string): string {
  return `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
}

async function expectOk(response: import('@playwright/test').APIResponse, description: string): Promise<void> {
  if (!response.ok()) {
    throw new Error(`${description} failed (${response.status()}): ${await response.text()}`);
  }
}

export async function createHackathon(
  adminApi: APIRequestContext,
  name = uniqueName('E2E-Hackathon'),
): Promise<{ hackathonId: string; name: string }> {
  const response = await adminApi.post('/api/hackathon', {
    data: { name, description: 'Created by Playwright E2E tests' },
  });
  await expectOk(response, 'create hackathon');
  return response.json();
}

export interface CreateEventOptions {
  name?: string;
  visibility?: 'PUBLIC' | 'PRIVATE';
  registrationKey?: string;
  active?: boolean;
  freezeTime?: string;
}

export async function createEvent(
  adminApi: APIRequestContext,
  hackathonId: string,
  options: CreateEventOptions = {},
): Promise<{ eventId: string; name: string; hackathon: string }> {
  const now = Date.now();
  const active = options.active ?? false;
  const startDateTime = new Date(active ? now - 30_000 : now + 10 * 60_000).toISOString();
  const duration = active ? 2 * 60 * 60 : 60 * 60;
  const visibility = options.visibility ?? 'PUBLIC';

  const response = await adminApi.post(`/api/hackathon/${hackathonId}/events`, {
    data: {
      name: options.name ?? uniqueName('E2E-Event'),
      registrationKey: visibility === 'PRIVATE' ? options.registrationKey ?? 'E2E-PRIVATE' : null,
      teamSizeLimit: 4,
      startDateTime,
      duration,
      description: 'Playwright E2E event',
      visibility,
      useIde: false,
      inPerson: false,
      allowedTech: ['Java', 'Python'],
      rules: 'E2E rules',
      tagline: 'Playwright E2E',
      firstPlacePrize: 1000,
      secondPlacePrize: 500,
      thirdPlacePrize: 250,
      totalPrizePool: 1750,
      ...(options.freezeTime ? { freezeTime: options.freezeTime } : {}),
    },
  });
  await expectOk(response, 'create event');
  return response.json();
}

export async function createLevel(
  adminApi: APIRequestContext,
  hackathonId: string,
): Promise<{ id: number; name: string; levelNumber: number }> {
  const response = await adminApi.post(`/api/hackathons/${hackathonId}/levels`, {
    data: {
      name: 'Level 1',
      levelNumber: 1,
      description: 'E2E scoring level',
    },
  });
  await expectOk(response, 'create level');
  return response.json();
}

export async function uploadSolver(adminApi: APIRequestContext, hackathonId: string): Promise<void> {
  const solverPath = path.resolve(__dirname, '../fixtures/e2e-solver.py');
  const response = await adminApi.post(`/api/storage/hackathons/${hackathonId}/solver`, {
    multipart: {
      file: {
        name: 'e2e-solver.py',
        mimeType: 'text/x-python',
        buffer: fs.readFileSync(solverPath),
      },
      notes: 'Playwright deterministic scoring solver',
    },
  });
  await expectOk(response, 'upload solver');
}

export async function uploadLevelResource(
  adminApi: APIRequestContext,
  hackathonId: string,
  levelId: number,
): Promise<void> {
  const resourcePath = path.resolve(__dirname, '../fixtures/level-resource.txt');
  const response = await adminApi.post(`/api/storage/hackathons/${hackathonId}/levels/${levelId}/files`, {
    multipart: {
      file: {
        name: 'level-resource.txt',
        mimeType: 'text/plain',
        buffer: fs.readFileSync(resourcePath),
      },
      fileType: 'TXT',
    },
  });
  await expectOk(response, 'upload level resource');
}

export async function registerForEvent(
  participantApi: APIRequestContext,
  eventId: string,
  regKey?: string,
): Promise<void> {
  const response = await participantApi.post(`/api/events/${eventId}/registered`, {
    data: regKey ? { regKey } : {},
  });
  await expectOk(response, 'register for event');
}

export async function createTeam(
  participantApi: APIRequestContext,
  eventId: string,
  teamName: string,
): Promise<{ teamId: string; teamName: string; joinCode: string }> {
  const response = await participantApi.post('/api/teams', {
    data: { teamName, eventId },
  });
  await expectOk(response, 'create team');
  return response.json();
}

export async function getMe(
  participantApi: APIRequestContext,
): Promise<{ userId: string; firstName: string; lastName: string; email: string }> {
  const response = await participantApi.get('/api/auth/me');
  await expectOk(response, 'get current user');
  return response.json();
}

export async function waitForScoredSubmission(
  participantApi: APIRequestContext,
  teamId: string,
  expectedScore: number,
): Promise<void> {
  await expect.poll(
    async () => {
      const response = await participantApi.get(`/api/scoring/teams/${teamId}/submissions`);
      if (!response.ok()) return `HTTP ${response.status()}`;
      const submissions = (await response.json()) as Array<{ status: string; score: number | string | null }>;
      const scored = submissions.find(
        submission =>
          submission.status === 'SCORED' &&
          submission.score !== null &&
          Number(submission.score) === expectedScore,
      );
      if (scored) return 'SCORED';
      const failed = submissions.find(submission => submission.status === 'FAILED');
      return failed ? 'FAILED' : submissions.map(s => s.status).join(',');
    },
    {
      timeout: 60_000,
      intervals: [500, 1_000, 2_000],
      message: `waiting for a SCORED submission with score ${expectedScore}`,
    },
  ).toBe('SCORED');
}

export async function freezeLeaderboard(adminApi: APIRequestContext, eventId: string): Promise<string> {
  const freezeTime = new Date(Date.now() - 250).toISOString();
  const response = await adminApi.put(`/api/admin/events/${eventId}`, {
    data: { freezeTime },
  });
  await expectOk(response, 'freeze leaderboard');
  return freezeTime;
}

export async function deleteHackathonQuietly(
  adminApi: APIRequestContext,
  hackathonId: string,
): Promise<void> {
  try {
    await adminApi.delete(`/api/hackathon/${hackathonId}`);
  } catch {
    // Test data uses unique names, so failed cleanup must not hide the real test result.
  }
}
