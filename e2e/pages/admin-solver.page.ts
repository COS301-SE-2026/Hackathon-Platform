import {Page,Locator,expect} from '@playwright/test';

export class AdminSolverPage {
    readonly page: Page;
    readonly uploadButton: Locator;
    readonly dropZone: Locator;
    readonly changeNotesInput: Locator;
    readonly uploadActivateButton: Locator;
    readonly backButton: Locator;
    readonly rescoreButton: Locator;


    constructor(page: Page){
        this.page = page;
        this.uploadButton = page.getByRole('button',{name:'Upload new version'});
        this.dropZone = page.locator('#fileInput');
        this.changeNotesInput = page.locator('#changeNotes');
        this.uploadActivateButton = page.getByRole('button',{name: 'Upload & activate'});
        this.backButton = page.getByRole('button',{name: 'Back to Hackathon'});
        this.rescoreButton = page.getByRole('button',{name: /Rescore all submissions/});

    }
    async goto(hackathonId: string){
        await this.page.goto(`/admin/hackathons/${hackathonId}/solver`);
        await this.waitForLoad();
    }
    async goBack(){
        await this.backButton.click();
    
    }
    async waitForLoad(){
        await expect(this.page.locator('.page-header')).toBeVisible({timeout:10000});
    }
    async uploadSolverVersion(filePath: string, changeNotes = ''){
        await this.uploadButton.click();
        await this.dropZone.setInputFiles(filePath);
        if (changeNotes) await this.changeNotesInput.fill(changeNotes);
        await this.uploadActivateButton.click();
    }

    async rescoreAll(){
        await this.rescoreButton.click();
    }
    versionRow(version:string): Locator{
        return this.page.locator('table tbody tr').filter({hasText: version});
    }

    async expectVersionVisible(version: string){
        await expect(this.versionRow(version)).toBeVisible();
    }

    async expectVersionStatus(version: string, status:string){
        const row = this.versionRow(version);
        const badge = row.locator('.status-badge');
        await expect(badge).toContainText(status);
    }
    async getVersionCount(): Promise<number>{
        return await this.page.locator('table tbody tr').count();
    }
    async expectRescoreSuccess(){
        await expect(this.page.locator('.rescore-success')).toBeVisible();
    }


}