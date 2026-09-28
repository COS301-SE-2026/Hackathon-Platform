import { test, expect } from '@playwright/test';
import { apiLogin, apiContext } from './fixtures/api';
import { users } from './fixtures/test-data';

test.describe('API auth boundaries', () => {
    test('admin token can reach an admin-only endpoint', async() => {
        const admin = await apiLogin(users.admin.email, users.admin.password);
        const api = await apiContext(admin.token);
        const res = await api.get('/api/admin/events');
        await api.dispose();
    });

    test('participant token is forbidden from admin only endpoints', async() => {
        const participant = await apiLogin(users.participant.email, users.participant.password);
        const api = await apiContext(participant.token);
        const res = await api.post('/api/hackathon', {data: {name:'Should not exist', description: 'denied' }});
        expect(res.status()).toBe(403);
        await api.dispose();
    });

    test('participant token cant create an event', async() => {
        const participant = await await apiLogin(users.participant.email, users.participant.password);
        const api = await apiContext(participant.token);
        const res = await api.post('/api/hackathon/00000000-0000-0000-0000-000000000000/events', {
            data:{ name: 'Dont create', teamSizeLimit: 4, description: 'shouldnt create'},
        });
        expect(res.status()).toBe(403);
        await api.dispose();
    });

    test('me returns the info of token', async () => {
        const participant = await apiLogin(users.participant.email, users.participant.password);
        const api = await apiContext(participant.token);
        const res = await api.get('/api/auth/me');
        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.role).toBe('PARTICIPANT');
        expect(body.email).toBe(users.participant.email.toLowerCase());
        await api.dispose();
    });

    test('requests without a token rejected', async() =>{
        const api = await apiContext();
        for(const path of ['/api/admin/events', '/api/auth/me']){
            expect([401, 403], path).toContain((await api.get(path)).status());
        }
        await api.dispose();
    });

    test('an incorrect token is rejected', async () => {
        const api = await apiContext('not.real.jwt');
        expect([401, 403]).toContain((await api.get('/api/admin/events')).status());
        await api.dispose();
    });
    test('login endpoint rejects wrong password with 401 and no token', async () => {
        const api = await apiContext();
        const res = await api.post('/api/auth/login', {data: {email: users.participant.email, password: 'this-!5Wrong123'}});
        expect(res.status()).toBe(401);
        expect((await res.json()).token).toBeUndefined();
        await api.dispose();
    });
});