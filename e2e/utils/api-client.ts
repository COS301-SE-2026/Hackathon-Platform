import { APIRequestContext, request } from '@playwright/test';
import * as dotenv from 'dotenv';
import * as path from 'path';

dotenv.config({ path: path.resolve(__dirname, '../.env') });

/**
 * API client used by e2e specs to seed data before a UI test
 * and tear down data after it
 */

const API_BASE_URL = process.env.E2E_API_BASE_URL;

export interface HackathonSeed {

  hackathonId: string;
  name: string;
  description?: string;

}

export interface EventSeed {

  eventId: string;
  name: string;
  hackathonId: string;
}

export interface LevelSeed {
  id: number;
  name: string;
  levelNumber: number;
  hackathonId: string;

}

export class ApiClient {
  private constructor(
    private readonly context: APIRequestContext,
    private readonly token: string
  ) {}

  static async loginAsAdmin(): Promise<ApiClient> {
    const email = process.env.E2E_ADMIN_EMAIL;
    const password = process.env.E2E_ADMIN_PASSWORD;

    if (!email || !password) {
        throw new Error('E2E_ADMIN_EMAIL / E2E_ADMIN_PASSWORD are not set');
    }

    const context = await request.newContext({ baseURL: API_BASE_URL });
    const res = await context.post('/api/auth/login', { data: { email, password } });
    if (!res.ok()) {
        throw new Error(`Admin login failed for API seeding: ${res.status()} ${await res.text()}`);
    }

    const body = await res.json();
    return new ApiClient(context, body.token);

  }

  private authHeaders() {
    return { Authorization: `Bearer ${this.token}` };

  }

  async createHackathon(name: string, description = ''): Promise<HackathonSeed> {
    const res = await this.context.post('/api/hackathon', {
        headers: this.authHeaders(),
        data: { name, description },
    });

    if(!res.ok()) {
        throw new Error(`Seed: failed to create hackathon "${name}": ${res.status()} ${await res.text()}`);

    }
    const body = await res.json();
    return { hackathonId: body.hackathonId, name: body.name, description: body.description };
  }

  async listHackathons(): Promise<HackathonSeed[]> {
    const res = await this.context.get('/api/hackathon', { headers: this.authHeaders() });
    if (!res.ok()) return [];
    return await res.json();

  }

  async deleteHackathon(hackathonId: string): Promise<void> {

    if (!hackathonId) return;
    const res = await this.context.delete(`/api/hackathon/${hackathonId}`, {
        headers: this.authHeaders(),
    });
    if (!res.ok() && res.status() !== 404) {
        console.warn(`Teardown: failed to delete hackathon ${hackathonId}: ${res.status()} ${await res.text()}`);

    }

  }

  async deleteHackathons(hackathonIds: string[]): Promise<void> {

    for(const id of hackathonIds) {
        await this.deleteHackathon(id);
    }
  }

  async findHackathonIdByName(name: string): Promise<string | undefined> {
    const all = await this.listHackathons();
    return all.find((h) => h.name === name)?.hackathonId;

  }

  async createEvent(
    hackathonId: string,
    overrides: Partial<{
        name: string;
        teamSizeLimit: number;
        startDateTime: string;
        duration: number;
        description : string;
        visibility: 'PUBLIC' | 'PRIVATE';
        registrationKey: string;
    }> = {}
  ): Promise<EventSeed> {
    const data = {
        name: overrides.name ?? `E2E Seed Event ${Date.now()}`,
        teamSizeLimit: overrides.teamSizeLimit ?? 4,
        startDateTime: overrides.startDateTime ?? new Date(Date.now() + 3600_000).toISOString(),
        duration: overrides.duration ?? 48 * 3600,
        description: overrides.description ?? 'Seeded via API for e2e test',
        visibility: overrides.visibility ?? 'PUBLIC',
        registrationKey: overrides.registrationKey,
    };
    const res = await this.context.post(`/api/hackathon/${hackathonId}/events`, {
        headers: this.authHeaders(),
        data,
    });
    if (!res.ok()) {
        throw new Error(`Seed: failed to create event "${data.name}": ${res.status()} ${await res.text()}`);
    }

    const body = await res.json();
    return { eventId: body.eventId, name: body.name, hackathonId };

  }

  async createLevel(
    hackathonId: string,
    name: string,
    levelNumber: number,
    description = ''
  ): Promise<LevelSeed> {
    const res = await this.context.post(`/api/hackathons/${hackathonId}/levels`, {
        headers: this.authHeaders(),
        data: { name, levelNumber, description },
    });

    if(!res.ok()) {
        throw new Error(`Seed: failed to create level "${name}": ${res.status()} ${await res.text()}`);
    }
    const body = await res.json();
    return { id: body.id, name: body.name, levelNumber: body.levelNumber, hackathonId };

  }

  async listLevels(hackathonId: string): Promise<LevelSeed[]> {

    const res = await this.context.get(`/api/hackathons/${hackathonId}/levels`, {
        headers: this.authHeaders(),
    });

    if(!res.ok()) return [];
    return await res.json();
  }

  async deleteLevel(levelId: number): Promise<void> {

    const res = await this.context.delete(`/api/levels/${levelId}`, {
        headers: this.authHeaders(),
    });

    if (!res.ok() && res.status() !== 404) {
        console.warn(`Teardown: failed to delete level ${levelId}: ${res.status()} ${await res.text()}`);

    }
  }

  async deleteLevels(levelIds: number[]): Promise<void> {
    for(const id of levelIds) {
        await this.deleteLevel(id);

    }
  }

  async findLevelIdByName(hackathonId: string, name: string): Promise<number | undefined> {

    const all = await this.listLevels(hackathonId);
    return all.find((l) => l.name === name)?.id;

  }

  async dispose(): Promise<void> {
    await this.context.dispose();
  }

}

  export function uniqueName(prefix: string) {
    const stamp = Date.now();
    const rand = Math.floor(Math.random() * 100000);
    return `${prefix} ${stamp}-${rand}`;
}