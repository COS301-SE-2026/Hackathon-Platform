import { test, expect } from './fixtures/auth.fixture';
import {CertificatesPage} from './pages/certificates.page';
import { VerifyPage } from './pages/verify.page';

test.describe('Admin > Certification', () =>{


async function seedTeams(
    api: import('@playwright/test').APIRequestContext,
    adminToken: string,
    eventId: string,
    count = 3,
) {
    for (let i =1; i<= count; i++){
        const res = await api.post(`/api/events/${eventId}/teams`,{
            headers: {Authorization: `Bearer ${adminToken}`},
            data:{
                name: `Team ${i}`,
                members: [{name: `Member ${i}`, email: `m${i}@e2e.test`}],
            },
        });
        if (!res.ok()){
            throw new Error(`Failed to seed team ${i}: ${res.status()} ${await res.text()}`);
        }
    }
}

test('create template, generate for all participants, verify via public page', async({
    adminPage,
    api,
    adminToken,
    eventId,

}) =>{
    await seedTeams(api, adminToken,eventId,3);

    const certs = new CertificatesPage(adminPage,eventId);
    await certs.goto();

    await certs.startNewTemplate('E2E Template All');
    await certs.saveTemplate();

    await certs.chooseScope('ALL_PARTICIPANTS');
    await certs.generate();
    await certs.waitForGenerationComplete();

    await expect(certs.issuedRows.first()).toBeVisible();

    const code = await certs.firstVerificationCode();
    const verify = new VerifyPage(adminPage);
    await verify.goto(code);
    await verify.expectValid();
});


test('generate for Top N only, verify the winner certificate', async({
    adminPage,
    api,
    adminToken,
    eventId,

}) =>{
    await seedTeams(api, adminToken,eventId,3);

    const certs = new CertificatesPage(adminPage,eventId);
    await certs.goto();

    await certs.startNewTemplate('E2E Template TopN');
    await certs.saveTemplate();

    await certs.chooseScope('TOP_N',2);
    await certs.generate();
    await certs.waitForGenerationComplete();

    const count = await certs.issuedRows.count();
    expect(count).toBeGreaterThan(0);
    expect(count).toBeLessThanOrEqual(2);

    const code = await certs.firstVerificationCode();
    const verify = new VerifyPage(adminPage);
    await verify.goto(code);
    await verify.expectValid();

    await verify.goto('not-a-real-code-xyz');
    await verify.expectInvalid();

});

test('selecting an existing template loads its name', async({
    adminPage,
    eventId,
    uniqueSuffix,

}) =>{
    const certs = new CertificatesPage(adminPage,eventId);
    const tplName = `Selectable ${uniqueSuffix}`;

    await certs.goto();
    await certs.startNewTemplate(tplName);
    await certs.saveTemplate();

    await certs.startNewTemplate('Scratch');
    await certs.selectTemplate(tplName);

    await expect(adminPage.locator('.template-name-input')).toHaveValue(tplName);

});

test('generate is disabled until a template is saved', async ({
    adminPage,
    eventId,
}) =>{
    const certs = new CertificatesPage(adminPage,eventId);
     await certs.goto();

     await expect(
        adminPage.getByRole('button', {name: /generate certificates/i}),

     ).toBeDisabled();
});

test('save template with a static label persists the label', async({
    adminPage,
    eventId,
    uniqueSuffix,

}) =>{
    const certs = new CertificatesPage(adminPage,eventId);
    await certs.goto();

    await certs.startNewTemplate(`Label ${uniqueSuffix}`);
    await certs.addStaticLabel('Presented by ACME');
    await certs.saveTemplate();

    await adminPage.reload();
    await certs.selectTemplate(`Label ${uniqueSuffix}`);
    await expect(adminPage.locator('.canvas-element', {hasText:'Presented by ACME'})).toBeVisible();
    
});

});