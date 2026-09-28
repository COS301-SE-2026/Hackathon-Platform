import * as dotenv from 'dotenv';
import * as path from 'path';

dotenv.config({ path: path.resolve(__dirname, '../.env'), quiet: true } as dotenv.DotenvConfigOptions);

function need(name: string): string {
    const v = process.env[name];
    if(!v){
        throw new Error(`Missing env var ${name}`);
    }
    return v;
}

export const users = {
    admin: {
        get email(){ return need('E2E_ADMIN_EMAIL'); },
        get password() { return need('E2E_ADMIN_PASSWORD');},
    },
    participant: {
        get email(){ return need('E2E_PARTICIPANT_EMAIL'); },
        get password() { return need('E2E_PARTICIPANT_PASSWORD');},
    },
};

export function uniqueSuffix(): string {
    return `${Date.now()}_${Math.floor(Math.random()*10000)}`;
}

export function randomEmail(prefix = 'e2e_user'): string {
    return `${prefix}_${uniqueSuffix()}@e2e-test.com`;
}

export function randomTeamName(): string {
    return `E2E Team ${uniqueSuffix()}`;
}

export function randomHackathonName(): string{
    return `E2E Hackathon ${uniqueSuffix()}`;
}

export function randomEventName(): string {
    return `E2E Event ${uniqueSuffix()}`;
}

export const VALID_PASSWORD = 'VeryStr0ng!123';