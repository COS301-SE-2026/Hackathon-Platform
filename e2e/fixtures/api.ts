import { APIRequestContext, request as pwRequest } from '@playwright/test';

export const API_ORIGIN = (process.env.API_URL || 'http://localhost:8080').replace(/\/api\/?$/, '').replace(/\/$/, '');
export interface AuthPayload { token?: string; role?: string; email?: string; [k: string]: unknown }

export async function apiLogin(email: string, password: string): Promise<AuthPayload> {
    const ctx = await pwRequest.newContext({ baseURL: API_ORIGIN });
    try{
        const res = await ctx.post('/api/auth/login', { data: { email: email.toLowerCase(), password }});
        if(!res.ok()) throw new Error(`apiLogin(${email}) failed: ${res.status()} ${await res.text()}`);
        return (await res.json()) as AuthPayload;
    } finally {
        await ctx.dispose();
    }
}

export async function apiContext(token?: string): Promise<APIRequestContext> {
    return pwRequest.newContext({
        baseURL: API_ORIGIN,
        extraHTTPHeaders: token ? { Authorization: `Bearer ${token}` } : {},
    });
}

export function apiRegister(ctx: APIRequestContext, u: { firstName: string, lastName: string, email: string, password: string }){
    return ctx.post('/api/auth/register', { data: u });
}