import {Page,Locator,expect} from '@playwright/test';

export class AdminHackathonsPage {
    readonly page: Page;
    readonly newHackathonButton: Locator;
    readonly nameInput: Locator;
    readonly descriptionInput: Locator;
    readonly problemStatementInput: Locator;
    readonly saveButton: Locator;
    readonly cancelButton: Locator;
    readonly loadingIndicator: Locator;
    readonly errorBanner: Locator;
    readonly searchInput: Locator;
    readonly gridViewButton: Locator;
    readonly listViewButton: Locator;


    constructor(page: Page){
        this.page = page;
        this.newHackathonButton = page.getByRole('button',{name: '+ Create Hackathon'});
        this.nameInput = page.locator('#hackathonName');
        this.descriptionInput = page.locator('#hackathonDescription');
        this.problemStatementInput = page.locator('#problemStatementFile');
        this.saveButton = page.getByRole('button',{name: /^Create Hackathon$|^Save Changes$/});
        this.cancelButton = page.getByRole('button',{name: 'Cancel'});
        this.loadingIndicator = page.locator('.loading');
        this.errorBanner = page.locator('.error-banner');
        this.searchInput = page.getByLabel('Search hackathons');
        this.gridViewButton = page.getByLabel('Grid view');
        this.listViewButton = page.getByLabel('List view');

     
        
    }
    async goto(){
        await this.page.goto('/admin/hackathons');
        await this.waitForLoad();
    }

    async waitForLoad(){
        await expect(this.loadingIndicator).not.toBeVisible({timeout:10000});
    }

    card(name:string): Locator{
        const escaped = name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
        return this.page.locator('.hackathon-card, .hackathon-row').filter({
            has: this.page.locator('.hackathon-name', { hasText: new RegExp(`^${escaped}\\s*$`) }),
        });
    }

    async createHackathon(name:string, description ='', problemStatementPath?: string){
        await this.newHackathonButton.click();
        await this.nameInput.fill(name);
        if (description) await this.descriptionInput.fill(description);
        if (problemStatementPath){
            await this.problemStatementInput.setInputFiles(problemStatementPath);

        }
        await this.saveButton.click();
        await this.waitForLoad();
    }

    async editHackathon(oldName:string, newName:string, description = ''){
        await  this.card(oldName).getByRole('button',{name: 'Edit hackathon'}).click();
        await this.nameInput.fill(newName);
        if (description) await this.descriptionInput.fill(description);
        await this.saveButton.click();
        await this.waitForLoad();
    }

    async deleteHackathon (name: string) {
        this.page.once('dialog', dialog => dialog.accept());
        await this.card(name).getByRole('button',{name:'Delete hackathon'}).click();
        
    }

    async cancelDeleteHackathon(name: string) {
        this.page.once('dialog', dialog => dialog.dismiss());
        await this.card(name).getByRole('button',{name: 'Delete hackathon'}).click();
    }

    async search(term: string){
        await this.searchInput.fill(term);
    }

    async setViewMode(mode: 'grid' | 'list') {
        if (mode === 'grid'){
            await this.gridViewButton.click();
        } else {
            await this.listViewButton.click();
        }
    }


    async expectHackathonVisible (name: string) {
        await expect(this.card(name)).toBeVisible({ timeout: 10000 });
        
    }
     async expectHackathonNotVisible (name: string) {
        await expect(this.card(name)).toHaveCount(0, {timeout: 10000 }); 
    }
    async getHackathonCount(): Promise<number> {
        return await this.page.locator('.hackathon-card, .hackathon-row').count();
    }
    async getHackathonNames(): Promise<string[]> {
        return await this.page.locator('.hackathon-name').allTextContents();
    }

    async expectEmptyState(text: string | RegExp){
        await expect(this.page.locator('.empty-state')).toContainText(text);
    }

}