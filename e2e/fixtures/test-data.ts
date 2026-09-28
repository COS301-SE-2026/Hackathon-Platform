export const users = {
    admin: {
        email: process.env.E2E_ADMIN_EMAIl,
        password: process.env.E2E_ADMIN_PASSWORD,
    },
    participant: {
        email: process.env.E2E_PARTICIPANT_EMAIL,
        password: process.env.E2E_PARTICIPANT_PASSWORD,
    },
};

export function uniqueSuffix(): string {
    return `${Date.now()}_${Math.floor(Math.random()*10000)}`;
}

export function randomEmail(prefix = 'e2e_user'): string {
    return `${prefix}_${uniqueSuffix()}@e2e-test.com`;
}

export function randomTeamName(): string {
    return `E2E Team ${uniqueSuffix()};`
}

export function randomHackathonName(): string{
    return `E2E Hackathon ${uniqueSuffix()}`;
}

export function randomEventName(): string {
    return `E2E Event ${uniqueSuffix()}`;
}

export const VALID_PASSWORD = 'VeryStr0ng!123';