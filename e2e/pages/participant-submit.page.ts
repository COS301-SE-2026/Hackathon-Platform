import{Page,Locator,expect} from '@playwright/test';

export class SubmissionsPage{
readonly page: Page;

readonly uploadSolutionTab: Locator;
readonly pageTitle: Locator;

readonly levelTabs: Locator;

readonly sourceFileInput: Locator;
readonly sourceFileName: Locator;
readonly solutionFileInput: Locator;
readonly solutionFileName: Locator;

readonly submitButton: Locator;
readonly successMessage: Locator;
readonly errorMessage: Locator;
readonly loader: Locator;




constructor(page:Page){
this.page=page;
  this.uploadSolutionTab = page.locator('button', { hasText: 'Upload Solution' });
this.pageTitle= page.locator('.submission-title');
this.levelTabs = page.locator('p-tab');

this.sourceFileInput = page.locator('#sourceUploader input[type="file"]');
this.sourceFileName = page.locator('#sourceUploader .selected-file-name');

this.solutionFileInput = page.locator('#solutionUploader input[type="file"]');
this.solutionFileName = page.locator('#solutionUploader .selected-file-name');
this.submitButton = page.locator('.submit-bar .primary-btn');

this.successMessage = page.locator('.empty-history:has-text("success")');
this.errorMessage = page.locator('.empty-history:has-text("error")');
this.loader= page.locator('.pi-spinner');


}


    async goto(eventId: string | number) {
      await this.page.goto(`/participant/events/${eventId}`);
      
      await this.uploadSolutionTab.click();
       await this.page.waitForSelector('.submission-page');
    }


    async uploadSourceFile(filePath: string) {
        await this.sourceFileInput.setInputFiles(filePath);
    }

    async uploadSolutionFile(filePath: string) {
        await this.solutionFileInput.setInputFiles(filePath);
    }


    async submit() {
        await this.submitButton.click();
    }

    
    async expectPageLoaded() {
       await expect(this.pageTitle).toHaveText('Submit Solution');
    }

    async expectLevelsVisible() {
        await expect(this.levelTabs.first()).toBeVisible();
    }
}




