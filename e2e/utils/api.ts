import { APIRequestContext, request } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';

export const ADMIN_STATE = path.resolve(__dirname, '../playwright/.auth/admin.json');
export const PARTICIPANT_STATE = path.resolve(__dirname, '../playwright/.auth/participant.json');
export const PARTICIPANT_2_STATE = path.resolve(__dirname, '../playwright/.auth/participant2.json');

const API_BASE_URL = process.env.E2E_API_URL ?? 'http://localhost:8080';

interface StorageStateFile {
  origins?: Array<{
    origin: string;
    localStorage?: Array<{ name: string; value: string }>;
  }>;
}

function tokenFromState(statePath: string): string {
  if (!fs.existsSync(statePath)) {
    throw new Error(`Authentication state not found: ${statePath}. Run the setup project first.`);
  }

  const state = JSON.parse(fs.readFileSync(statePath, 'utf8')) as StorageStateFile;
  for (const origin of state.origins ?? []) {
    const token = origin.localStorage?.find(item => item.name === 'token')?.value;
    if (token) return token;
  }

  throw new Error(`No localStorage token found in ${statePath}`);
}

export async function apiForState(statePath: string): Promise<APIRequestContext> {
  const token = tokenFromState(statePath);
  return request.newContext({
    baseURL: API_BASE_URL,
    extraHTTPHeaders: {
      Authorization: `Bearer ${token}`,
      Accept: 'application/json',
    },
  });
}
