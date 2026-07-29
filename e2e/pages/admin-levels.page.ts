import {Page,Locator,expect} from '@playwright/test';

export class AdminLevelsPage {
    readonly page: Page;
    readonly addLevelButton: Locator;
    readonly levelNameInput: Locator;
    readonly levelNumberInput: Locator;
    readonly levelDescriptionInput: Locator;
    readonly backButton: Locator;
    readonly saveButton: Locator;
    readonly cancelButton: Locator;
    readonly loadingIndicator: Locator;
    

    constructor(page: Page){
        this.page = page;
        this.addLevelButton = page.getByRole('button',{name: '+Add New Level'});
        this.levelNameInput = page.locator('#levelName');
        this.levelNumberInput = page.locator('#levelNumber');
        this.levelDescriptionInput = page.locator('#levelDescription');
        this.saveButton = page.getByRole('button',{name: /Add Level|Save changes/});
        this.cancelButton = page.getByRole('button',{name:'Cancel'});
        this.backButton = page.getByRole('button',{name:'Back to Hackathon'});
        this.loadingIndicator = page.locator('.empty-state:has-text("Loading levels...")');

    }
    async goto(hackathonId: string){
        await this.page.goto(`/admin/hackathons/${hackathonId}/levels`);
        await this.waitForLoad();
    }
    async waitForLoad(){
        await expect(this.loadingIndicator).not.toBeVisible({timeout: 10000});
    }
    async goBack(){
        await this.backButton.click();
    }
    levelRow(name:string): Locator {
        return this.page.locator('.level-card').filter({hasText: name});
    }

    async addLevel(name:string, levelNumber: number, description =''){
        await this.addLevelButton.click();
        await this.levelNameInput.fill(name);
        await this.levelNumberInput.fill(levelNumber.toString());
        if (description) await this.levelDescriptionInput.fill(description);
        await this.saveButton.click();
        await this.waitForLoad();
    }

    async editLevel(name:string, newName: string, levelNumber:number, description=''){
        await this.levelRow(name).getByRole('button',{name: 'Edit'}).click();
        await this.levelNameInput.fill(newName);
        await this.levelNumberInput.fill(levelNumber.toString());
        if (description) await this.levelDescriptionInput.fill(description);
        await this.saveButton.click();
        await this.waitForLoad();
    }
    async deleteLevel(name:string){
        await this.levelRow(name).getByRole('button',{name: 'Edit'}).click();
        await this.page.getByRole('button',{name: 'Delete Level'}).click();
    }

    async manageFiles(name:string){
        await this.levelRow(name).getByRole('button',{name: 'Manage Files'}).click();
    }
    async expectLevelVisible(name:string){
        await expect(this.levelRow(name)).toBeVisible();
    }
    async expectLevelNotVisible(name:string){
        await expect(this.levelRow(name)).toHaveCount(0);
    }
    async getLevelCount(): Promise<number>{
        return await this.page.locator('.level-card').count();
    }
    async getLevelNames(): Promise<string[]>{
        return await this.page.locator('.level-name').allTextContents();
    }

}