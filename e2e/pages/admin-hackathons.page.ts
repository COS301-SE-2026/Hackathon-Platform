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

    constructor(page: Page){
        this.page = page;
        this.newHackathonButton = page.getByRole('button',{name: '+ New Hackathon'});
        this.nameInput = page.locator('#hackathonName');
        this.descriptionInput = page.locator('#hackathonDescription');
        this.problemStatementInput = page.locator('#problemStatementFile');
        this.saveButton = page.getByRole('button',{name: /Save/});
        this.cancelButton = page.getByRole('button',{name: 'Cancel'});
        this.loadingIndicator = page.locator('.loading');
        this.errorBanner = page.locator('.error-banner');
        
    }
    async goto(){
        await this.page.goto('/admin/hackathons');
        await this.waitForLoad();
    }

    async waitForLoad(){
        await expect(this.loadingIndicator).not.toBeVisible({timeout:10000});
    }

    card(name:string): Locator{
        return this.page.locator('.hackathon-card').filter({hasText:name});
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
        await  this.card(oldName).getByRole('button',{name: 'Edit'}).click();
        await this.nameInput.fill(newName);
        if (description) await this.descriptionInput.fill(description);
        await this.saveButton.click();
        await this.waitForLoad();
    }

    async deleteHackathon (name: string) {
        await this.card(name).getByRole('button',{name:'Delete'}).click();
        
    }

    async navigateToEvents (name: string) {
        await this.card(name).getByRole('button',{name:'View Events'}).click();
        
    }
    async navigateToCreateEvent (name: string) {
        await this.card(name).getByRole('button',{name:'+ Create Event'}).click();
        
    }
    async navigateToLevels (name: string) {
        await this.card(name).getByRole('button',{name:'Levels'}).click();
        
    }
    async navigateToSolver (name: string) {
        await this.card(name).getByRole('button',{name:'Solver'}).click();  
    }

    async expectHackathonVisible (name: string) {
        await expect(this.card(name)).toBeVisible();
        
    }
     async expectHackathonNotVisible (name: string) {
        await expect(this.card(name)).toHaveCount(0); 
    }
    async getHackathonCount(): Promise<number> {
        return await this.page.locator('.hackathon-card').count();
    }
    async getHackathonNames(): Promise<string[]> {
        return await this.page.locator('.hackathon-name').allTextContents();
    }

}