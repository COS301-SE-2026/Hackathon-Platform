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

}