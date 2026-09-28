import {Page,Locator,expect} from '@playwright/test';

export interface CreateEventOptions {

    name: string;
    startDate: string;
    startTime: string;
    duration: number;
    teamSizeLimit: number;
    description?: string;
    visibility?: 'PUBLIC' | 'PRIVATE';
    registrationKey?: string;
}

export class AdminEventsPage {
    readonly page: Page;
    private hackathonId?: string;
    readonly newEventButton: Locator;
    readonly searchInput: Locator;
    readonly statusFilter: Locator;
    readonly visibilityFilter: Locator;
    readonly loadingIndicator: Locator;
    readonly errorBanner: Locator;

    readonly eventNameInput: Locator;
    readonly startDateInput: Locator;
    readonly startTimeInput: Locator;
    readonly durationInput: Locator;
    readonly teamSizeInput: Locator;
    readonly descriptionInput: Locator;
    readonly publicOption: Locator;
    readonly privateOption: Locator;
    readonly registrationKeyInput: Locator;
    readonly createSaveButton: Locator;
    readonly createCancelButton: Locator;
    readonly createErrorMessage: Locator;

    constructor(page: Page){
        this.page = page;
        this.newEventButton = page.getByRole('button',{name: '+ Create Event'});
        this.searchInput = page.getByLabel('Search events');
        this.statusFilter = page.getByLabel('Filter by status');
        this.loadingIndicator = page.locator('.empty-state', { hasText: 'Loading events...' });
        this.errorBanner = page.locator('.error-banner');

        this.eventNameInput = page.locator('#eventName');
        this.startDateInput = page.locator('#startDate');
        this.startTimeInput = page.locator('#startTime');
        this.durationInput = page.locator('#duration');
        this.teamSizeInput = page.locator('#teamSizeLimit');
        this.descriptionInput  = page.locator('#description');

        this.publicOption = page.locator('.access-option').filter({ hasText: 'Public' });
        this.privateOption = page.locator('.access-option').filter({ hasText: 'Private' });

        this.registrationKeyInput = page.locator('#registrationKey');
        this.createSaveButton = page.getByRole('button',{name: 'Create Event'});
        this.createCancelButton = page.locator('.create-event-page').getByRole('button', { name: 'Cancel' });
        this.createErrorMessage = page.locator('.error-message');
        
        
    }
    async goto(hackathonId: string){
        this.hackathonId = hackathonId;
            await this.page.goto(`/admin/hackathons/${hackathonId}/events`);   
            await this.waitForLoad();
    }

    async waitForLoad(){
        await expect(this.loadingIndicator).not.toBeVisible({timeout: 10000});
    }

    eventRow(name: string): Locator {

        const escaped = name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
        return this.page.locator('.event-row').filter({has: this.page.locator('.event-name', { hasText: new RegExp(`^${escaped}\\s*S`)}),});
    }

    async gotoCreateForm() {
        if(!this.hackathonId) {
            throw new Error('AdminEventsPage.gotoCreateForm() requires goto(hackathonId) to have been called first.');

        }

        await this.newEventButton.click();
        await this.page.waitForURL(/\/admin\/hackathons\/.+\/events\/create$/);
    }

    async fillCreateForm(opts: CreateEventOptions){

        await this.eventNameInput.fill(opts.name);
        await this.startDateInput.fill(opts.startDate);
        await this.startTimeInput.fill(opts.startTime);
        await this.durationInput.fill(opts.duration.toString());
        await this.teamSizeInput.fill(opts.teamSizeLimit.toString());

        if (opts.description) await this.descriptionInput.fill(opts.description);
        if (opts.visibility === 'PRIVATE') {
            await this.privateOption.click();
            await expect(this.registrationKeyInput).toBeVisible();
            if (opts.registrationKey) await this.registrationKeyInput.fill(opts.registrationKey);

        }
    }

    async submitCreateForm(){
        await this.createSaveButton.click();
    }

    async createEvent(opts: CreateEventOptions){
        
        await this.gotoCreateForm();
        await this.fillCreateForm(opts);
        await this.submitCreateForm();
        await this.page.waitForURL(/\/admin\/hackathons\/.+\/events$/ , { timeout: 15000 });
        await this.waitForLoad();
    }

    async cancelCreateForm(){
        await this.createCancelButton.click();

    }

    async searchEvents(query:string){
        await this.searchInput.fill(query);
    }

    async filterByStatus(status: 'ALL' | 'LIVE' | 'UPCOMING' | 'COMPLETED' | 'CANCELED'){
        await this.statusFilter.selectOption(status);

    }

    async navigateToManage(name:string){

        await this.eventRow(name).getByRole('button',{name:'Go to Event'}).click();
        await this.page.waitForURL(/\/admin\/hackathons\/.+\/events\/.+\/dashboard$/);
        const eventId = this.page.url().match(/\/events\/([^/]+)\/dashboard$/)?.[1] || '';
        await this.page.goto(`/admin/hackathons/${this.hackathonId}/events/${eventId}/manage`);

    }


    async expectEventVisible(name:string){
        await expect(this.eventRow(name)).toBeVisible();

    }
    async expectEventNotVisible(name:string){
        await expect(this.eventRow(name)).toHaveCount(0);
        
    }

    async getEventStatus(name: string): Promise<string>{
        const row = this.eventRow(name);
        return await row.locator('.status-pill').textContent() || '';
    }

    async getEventVisibility(name: string): Promise<string>{
        const row = this.eventRow(name);
        return await row.locator('.type-pill').textContent() || '';
    }

    async expectEmptyState(text: string | RegExp){
        await expect(this.page.locator('.empty-state')).toContainText(text);
    }


}