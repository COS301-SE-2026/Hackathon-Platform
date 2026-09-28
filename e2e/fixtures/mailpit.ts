import { request as pwRequest } from '@playwright/test';

export const  MAILPIT_URL = process.env.MAILPIT_URL?.replace(/\/$/, '');

export async function waitForVerificationToken(email: string, timeoutMs = 20_000): Promise<string> {
    if(!MAILPIT_URL) {
        throw new Error('MAILPIT_URL is not set');
    }
    const ctx = await pwRequest.newContext({ baseUrl: MAILPIT_URL });
    try{
        const timeout = Date.now()+timeoutMs;
        while(Date.now()< timeout ){
            const search = await ctx.get('/api/v1/search', { params: { query: `to:${email}` }});
            if(search.ok()){
                const {messages} = (await search.json()) as { messages?: { ID: string }[] };
                if(messages?.length){
                    const msg = await (await ctx.get(`/api/v1/message/${messages[0].ID}`)).json();
                    const body: string = `${msg.Text ?? ''} ${msg.HTML ?? ''}`;
                    const m = body.match(/verify-email\?token=([A-Za-z0-9_\-%.]+)/);
                    if(m){
                        return decodeURIComponent(m[1]);
                    }
                }
            }
                await new Promise((r) => setTimeout(r, 500));
            }
            throw new Error(`No verification email for ${email} within ${timeoutMs}ms`);
        } finally {
            await ctx.dispose();
    }
}