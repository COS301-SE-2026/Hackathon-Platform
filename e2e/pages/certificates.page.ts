import {Page, Locator,expect} from '@playwright/test'

export class CertificatesPage{
    constructor(private page: Page, private eventId: string){}
        
    
    
async goto(){
    await this.page.goto(`/admin/events/${this.eventId}/certificates`);
    await this.page.waitForSelector('.certificates-page');

}

async startNewTemplate(name: string){
    await this.page.getByRole('button',{name:/^\s*new\s*$/i}).click();
    await this.page.locator('.template-name-input').fill(name);
}

async saveTemplate(){
    await this.page.getByRole('button',{name:/save template/i}).click();
    await expect(this.page.locator('.success-message')).toBeVisible();
}

async selectTemplate(name:string){
    await this.page.locator('.template-list li', {hasText: name}).click();
}

async addStaticLabel(text: string){
    await this.page.getByRole('button',{name: /static label/i}).click();
    await this.page.locator('.property-panel input[type="text"]').fill(text);
}

async chooseScope(scope: 'ALL_PARTICIPANTS' | 'TOP_N', topN?: number){
    if (scope === 'ALL_PARTICIPANTS'){
        await this.page.locator('.toggle-option',{hasText: /all participants/i}).click();
    } else {
       await this.page.locator('.toggle-option',{hasText: /top n teams/i}).click();
       await this.page.locator('.top-n-input input').fill(String(topN?? 3)); 
    }
}

async generate(){
    await this.page.getByRole('button', {name: /generate certificates/i}).click();

}

async waitForGenerationComplete(){
    await expect(this.page.locator('.run-progress .progress-label')).toContainText(/COMPLETED|FAILED/,{
        timeout: 60_000,
    });
}

get issuedRows(): Locator{
    return this.page.locator('.issued-table tbody tr');
}

async firstVerificationCode(): Promise<string>{
    const row = this.issuedRows.first();
    await row.waitFor();
    const code = await row.locator('.code-cell').innerText();
    return code.trim();
}

}