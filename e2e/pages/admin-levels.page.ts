import {Page,Locator,expect} from '@playwright/test';
import { escapeRegExp } from '../utils/regex';

export class AdminLevelsPage {
    readonly page: Page;
    readonly addLevelButton: Locator;
    readonly levelNameInput: Locator;
    readonly levelNumberInput: Locator;
    readonly levelDescriptionInput: Locator;
    readonly backButton: Locator;
    readonly saveButton: Locator;
    readonly modalCancelButton: Locator;
    readonly modalDeleteButton: Locator;
    readonly loadingIndicator: Locator;
    readonly errorBanner: Locator;
    readonly modalError: Locator;

    readonly fileUploadInput: Locator;
    readonly closeFilesModalButton: Locator;
    readonly fileError: Locator;
    

    constructor(page: Page){
        this.page = page;
        this.addLevelButton = page.getByRole('button',{name: '+ Create Level'});
        this.levelNameInput = page.locator('#levelName');
        this.levelNumberInput = page.locator('#levelNumber');
        this.levelDescriptionInput = page.locator('#levelDescription');
        this.saveButton = page.locator('.modal-actions').getByRole('button',{name: /Add Level|Save changes/});
        this.modalCancelButton = page.locator('.modal-actions').getByRole('button',{name:'Cancel'});
        this.modalDeleteButton = page.locator('.modal-actions').getByRole('button',{name:'Delete Level'});
        this.backButton = page.getByRole('button',{name:'Back to Hackathons'});
        this.loadingIndicator = page.locator('.empty-state', { hasText: 'Loading levels...' });
        this.errorBanner = page.locator('main .error-banner');
        this.modalError = page.locator('.modal-card .error-banner');

        this.fileUploadInput = page.locator('#fileUpload');
        this.closeFilesModalButton = page.locator('.modal-actions').getByRole('button', {name: 'Close'});
        this.fileError = page.locator('.modal-card .error-banner');
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
        const escaped = escapeRegExp(name);
        return this.page.locator('.level-row').filter({
            has: this.page.locator('.level-name', { hasText: new RegExp(String.raw`^Level\s+\d+\s*:\s*${escaped}\s*$`)}),
        });
    }

    async addLevel(name:string, levelNumber: number, description =''){
        await this.addLevelButton.click();
        await this.levelNameInput.fill(name);
        await this.levelNumberInput.fill(levelNumber.toString());
        if (description) await this.levelDescriptionInput.fill(description);
        await this.saveButton.click();
    }

    async editLevel(name:string, newName: string, levelNumber:number, description=''){
        await this.levelRow(name).getByRole('button',{name: 'Edit level'}).click();
        await this.levelNameInput.fill(newName);
        await this.levelNumberInput.fill(levelNumber.toString());
        if (description) await this.levelDescriptionInput.fill(description);
        await this.saveButton.click();
    }

    async deleteLevel(name:string){

        this.page.once('dialog', dialog => dialog.accept());
        await this.levelRow(name).getByRole('button' ,{name: 'Delete level'}).click();
    }

    async cancelDeleteLevel(name: string){

        this.page.once('dialog', dialog => dialog.dismiss());
        await this.levelRow(name).getByRole('button',{name: 'Delete level'}).click();
    }

    async uploadStarterZip(name: string, filePath: string){
        const row = this.levelRow(name);
        await row.locator('input[type="file"]').setInputFiles(filePath);
    }

    async openManageFiles(name: string) {
        await this.levelRow(name).getByRole('button',{name: 'View Level'}).click();
        await expect(this.page.locator('.modal-card', { hasText: 'Manage Files' })).toBeVisible();

    }

    async uploadLevelFile(filePath: string){
        await this.fileUploadInput.setInputFiles(filePath);
    }

    async closeManageFiles(){
        await this.closeFilesModalButton.click();
    }

    async dragLevelTo(name: string, targetName: string){

        const source = this.levelRow(name).locator('.drag-handle');
        const target = this.levelRow(targetName);

        const sourceBox = await source.boundingBox();
        const targetBox = await target.boundingBox();
        if (!sourceBox || !targetBox) {
            
            throw new Error(`Could not resolve bounding boxes for drag from "${name}" to "${targetName}"`);
        }

        const startX = sourceBox.x + sourceBox.width / 2;
        const startY = sourceBox.y + sourceBox.height / 2;
        const endX = targetBox.x + targetBox.width / 2;
        const endY = targetBox.y + targetBox.height / 2;

        await this.page.mouse.move(startX, startY);
        await this.page.mouse.down();
        await this.page.mouse.move(startX, startY + (endY > startY ? 5 : -5), { steps: 2 });
        await this.page.mouse.move(endX, endY, { steps: 10 });
        await this.page.mouse.move(endX, endY, { steps: 2});
        await this.page.mouse.up();

        await this.page.locator('.cdk-drag-preview, .cdk-drag-placeholder').first()
            .waitFor({ state: 'detached', timeout: 5000})
            .catch(() => {});
    }


    async expectLevelVisible(name:string){

        await expect(this.levelRow(name)).toBeVisible();
    }

    async expectLevelNotVisible(name:string) {
        await expect(this.levelRow(name)).toHaveCount(0);
    }
    
    async getLevelCount(): Promise<number>{
        return await this.page.locator('.level-row:not(.cdk-drag-preview):not(.cdk-drag-placeholder)').count();
    }

    async getOrderedLevelNames(): Promise<string[]>{
        
        const raw = await this.page
            .locator('.level-row:not(.cdk-drag-preview):not(.cdk-drag-placeholder) .level-name')
            .allTextContents();
        
            return raw.map((t) => t.replace(/^Level\s+\d+\s*:\s*/, '').trim());

    }

}