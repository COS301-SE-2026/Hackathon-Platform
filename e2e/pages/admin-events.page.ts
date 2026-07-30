import {Page,Locator,expect} from '@playwright/test';

export class AdminEventsPage {
    readonly page: Page;
    private hackathonId?: string;
    readonly newEventButton: Locator;
    readonly searchInput: Locator;
    readonly statusFilter: Locator;
    readonly visibilityFilter: Locator;
    readonly loadingIndicator: Locator;
    readonly eventNameInput: Locator;
    readonly startDateInput: Locator;
    readonly durationInput: Locator;
    readonly teamSizeInput: Locator;
    readonly descriptionInput: Locator;
    readonly saveButton: Locator;
    readonly cancelButton: Locator;
    readonly backButton: Locator;

    constructor(page: Page){
        this.page = page;
        this.newEventButton = page.getByRole('button',{name: '+ New Event'});
        this.searchInput = page.locator('.search-input');
        this.statusFilter = page.locator('p-select[placeholder ="All statuses"]');
        this.visibilityFilter = page.locator('p-select[placeholder ="All visibility"]');
        this.loadingIndicator = page.locator('.loading');
        this.eventNameInput = page.locator('#eventName');
        this.startDateInput = page.locator('#startDate');
        this.durationInput = page.locator('#duration');
        this.teamSizeInput = page.locator('#teamSizeLimit');
        this.descriptionInput  = page.locator('textarea[name = "description"]');
        this.saveButton = page.getByRole('button',{name: 'Create Event'});
        this.cancelButton = page.getByRole('button',{name: 'Cancel'});
        this.backButton = page.locator('.btn-secondary').filter({hasText: 'Back to Events'});
        
        
    }
    async goto(hackathonId?: string){
        this.hackathonId = hackathonId;
        if (hackathonId){
            await this.page.goto(`/admin/hackathons/${hackathonId}/events`);   
        }else {
            await this.page.goto('/admin/events');

        }
        await this.waitForLoad();
    }

    async waitForLoad(){
        await expect(this.loadingIndicator).not.toBeVisible({timeout: 10000});
    }

    eventRow(name: string): Locator {
        return this.page.locator('.p-datatable-tbody tr').filter({hasText: name});
    }

    async createEvent(name:string, startDate: string, duration:number, teamSize: number, description = ''){
        if (!this.hackathonId) {
            throw new Error('AdminEventsPage.createEvent() requires goto(hackathonId) to have been called first.');
        }
        await this.page.goto(`/admin/hackathons/${this.hackathonId}/events/create`);
        await this.eventNameInput.fill(name);
        await this.startDateInput.fill(startDate);
        await this.durationInput.fill(duration.toString());
        await this.teamSizeInput.fill(teamSize.toString());
        if (description) await this.descriptionInput.fill(description);
        await this.saveButton.click();
        await this.page.waitForURL(/\/admin\/hackathons\/.+\/events$/);
        await this.waitForLoad();
    }
    async searchEvents(query:string){
        await this.searchInput.fill(query);
        await this.searchInput.press('Enter');
    }

    async navigateToManage(name:string){
        await this.eventRow(name).getByRole('button',{name:'Manage & Edit'}).click();

    }
    async navigateToLevels(name:string){
        await this.eventRow(name).getByRole('button',{name:'Levels'}).click();
        
    }
    async navigateToSolver(name:string){
        await this.eventRow(name).getByRole('button',{name:'Solver'}).click();
    }
    async goBack(){
        await this.backButton.click();
    }

    async expectEventVisible(name:string){
        await expect(this.eventRow(name)).toBeVisible();

    }
    async expectEventNotVisible(name:string){
        await expect(this.eventRow(name)).toHaveCount(0);
        
    }
    async getEventStatus(name: string): Promise<string>{
        const row = this.eventRow(name);
        return await row.locator('.p-tag').textContent() || '';
    }
}