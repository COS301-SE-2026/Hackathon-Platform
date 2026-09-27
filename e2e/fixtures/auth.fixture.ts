import {test as base, request, APIRequestContext, Page} from '@playwright/test';
import * as dotenv from 'dotenv';
import * as path from 'path';

dotenv.config({path: path.resolve(__dirname, '../.env')}) ;

const API_URL = process.env.E2E_API_URL || 'http://localhost:8080';
const ADMIN_STATE = path.resolve(__dirname, '../../playwright/.auth/admin.json');
const PARTICIPANT_STATE = path.resolve(__dirname, '../../playwright/.auth/participant.json');

type Fixtures = {
    adminPage: Page;
    participantPage: Page;
    api: APIRequestContext;
    adminToken: string;
    eventId: string;
    uniqueSuffix: string;
};

export const test = base.extend<Fixtures>({
    uniqueSuffix: async({}, use) =>{
        await use(`${Date.now()}-${Math.floor(Math.random() * 1e6)}`);

    },

    api: async ({}, use) =>{
        const ctx = await request.newContext({baseURL: API_URL});
        await use(ctx);
        await ctx.dispose();
    },

    adminToken: async ({ api }, use) =>{
        const res = await api.post('/api/auth/login', {
            data: {
                email: process.env.E2E_ADMIN_EMAIL,
                password: process.env.E2E_ADMIN_PASSWORD,

            },
        });
        if (!res.ok()){
            throw new Error(`Admin login failed: ${res.status()} ${await res.text()}`);
        }

        const body = await res.json();

        const token = body.token ?? body.accessToken ?? body.jwt;
        if (!token){
            throw new Error('Admin login response did not contain a token');

        }
        await use(token);
    },

    eventId: async ({ api, adminToken, uniqueSuffix }, use) => {
        const hackathonRes = await api.post('/api/hackathons',{
            headers: { Authorization: `Bearer ${adminToken}`},
            data: { name: `E2E Hackathon ${uniqueSuffix}`},
        });
        if (!hackathonRes.ok()){
            throw new Error(`Create hackathon failed: ${hackathonRes.status()}`);
        }

        const {hackathonId} = await hackathonRes.json();
        const eventRes = await api.post(`/api/hackathons/${hackathonId}/events`, {
            headers: { Authorization: `Bearer ${adminToken}`},
            data: {
                name: `E2E Event ${uniqueSuffix}`,
                description: 'Playwright E2E',
                status: 'PUBLISHED',
            },
        });

         if (!eventRes.ok()){
            throw new Error(`Create event failed: ${eventRes.status()}`);
        }

        const {eventId} = await eventRes.json();
        await use(eventId);

        await api 
        .delete(`/api/events/${eventId}`,{
            headers: {Authorization: `Bearer ${adminToken}`},
        })
        .catch(() => {});

    },

    adminPage: async ({ browser }, use) =>{
        const ctx = await browser.newContext({ storageState: ADMIN_STATE});
        const page = await ctx.newPage();
        await use(page);
        await ctx.close();
    },
    participantPage: async ({ browser }, use) =>{
        const ctx = await browser.newContext({ storageState: PARTICIPANT_STATE});
        const page = await ctx.newPage();
        await use(page);
        await ctx.close();
    },

});
export { expect } from '@playwright/test';
