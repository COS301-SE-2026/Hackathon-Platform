import {Page, Locator,expect} from '@playwright/test'

export class VerifyPage{
    constructor(private page: Page){}

async goto(code:string){
    await this.page.goto(`/verify/${code}`);
    await this.page.waitForLoadState('domcontentloaded')
}
async expectValid(){
    await expect(this.page.locator('.verify-valid, .certificate-valid')).toBeVisible();
}

async expectInvalid(){
    await expect(this.page.locator('.verify-invalid, .certificate-invalid')).toBeVisible();
}

async expectRecipient(name: string){
    await expect(this.page.locator('.verify-recipient')).toContainText(name);
}
}


