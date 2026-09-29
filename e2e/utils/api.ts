import { APIRequestContext, request } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';

export const ADMIN_STATE = path.resolve(__dirname, '../../playwright/.auth/admin.json');
export const PARTICIPANT_STATE = path.resolve(__dirname, '../../playwright/.auth/participant.json');
export const PARTICIPANT_2_STATE = path.resolve(__dirname, '../../playwright/.auth/participant2.json');

const API_BASE_URL = process.env.E2E_API_BASE_URL ?? process.env.E2E_API_URL ?? 'http://localhost:8080';
interface StorageStateFile {
  origins?: Array<{
    origin: string;
    localStorage?: Array<{ name: string; value: string }>;
  }>;
}

interface AuthResponse {
  token: string;
  role: string;
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

async function loginAdminApi(): Promise<APIRequestContext> {
  const email = process.env.E2E_ADMIN_EMAIL;
  const password = process.env.E2E_ADMIN_PASSWORD;

  if (!email || !password) {
    throw new Error('Missing E2E_ADMIN_EMAIL or E2E_ADMIN_PASSWORD');
  }

  const loginContext = await request.newContext({
    baseURL: API_BASE_URL,
  });

  try {
    const response = await loginContext.post('/api/auth/login', {
      data: {
        email,
        password,
      },
    });

    const responseText = await response.text();

    if (!response.ok()) {
      throw new Error(
        `Admin API login failed (${response.status()}): ${responseText}`,
      );
    }

    const auth = JSON.parse(responseText) as AuthResponse;

    if (!auth.token) {
      throw new Error('Admin API login returned no JWT token');
    }

    if (auth.role !== 'ADMIN' && auth.role !== 'SUPERADMIN') {
      throw new Error(
        `E2E admin account has wrong role: ${auth.role ?? 'UNKNOWN'}`,
      );
    }

    return request.newContext({
      baseURL: API_BASE_URL,
      extraHTTPHeaders: {
        Authorization: `Bearer ${auth.token}`,
        Accept: 'application/json',
      },
    });
  } finally {
    await loginContext.dispose();
  }
}

export async function apiForState(statePath: string): Promise<APIRequestContext> {
  if (path.resolve(statePath) === path.resolve(ADMIN_STATE)) {
    return loginAdminApi();
  }

  const token = tokenFromState(statePath);

  return request.newContext({
    baseURL: API_BASE_URL,
    extraHTTPHeaders: {
      Authorization: `Bearer ${token}`,
      Accept: 'application/json',
    },
  });
}