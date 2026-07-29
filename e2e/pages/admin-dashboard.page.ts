import {Page,Locator,expect} from '@playwright/test';

export class AdminDashboardPage {
    readonly page: Page;
    readonly statCards: Locator;
    readonly activeEventsStat: Locator;
    readonly totalParticipantsStat: Locator;
    readonly submissionsTodayStat: Locator;
    readonly eventsList: Locator;
    readonly submissionsTable: Locator;
    readonly newEventButton: Locator;
    

    constructor(page: Page){
        this.page = page;
        this.statCards = page.locator('.stat-card');
        this.activeEventsStat = page.locator('.stat-card .stat-value.purple');
        this.totalParticipantsStat = page.locator('.stat-card .stat-value.green');
        this.submissionsTodayStat = page.locator('.stat-card .stat-value.orange');
        this.eventsList = page.locator('.events-list-card');
        this.submissionsTable = page.locator('.table-card');
        this.newEventButton = page.getByRole('button',{name: '+ New Event'});
    }
    async goto(){
        await this.page.goto('/admin/dashboard');
        await this.waitForLoad();
    }
     async waitForLoad(){
        await expect(this.statCards.first()).toBeVisible({timeout:10000});
    }
    async clickNewEvent(){
        await this.newEventButton.click();
    }
    async getActiveEvents(): Promise<string>{
        return await this.activeEventsStat.textContent() || '0';
    }
    async getTotalParticipants(): Promise<string>{
        return await this.totalParticipantsStat.textContent() || '0';
    }
    async getSubmissionsToday(): Promise<string>{
        return await this.submissionsTodayStat.textContent() || '0';
    }

    async expectStatsVisible(){
        await expect(this.statCards.first()).toBeVisible();
    }
    async expectEventsListVisible(){
        await expect(this.eventsList).toBeVisible();
    }
    async expectSubmissionsTableVisible(){
        await expect(this.submissionsTable).toBeVisible();
    }

    async expectEventInList(name:string){
        await expect(this.page.locator('.event-item').filter({hasText:name})).toBeVisible();
    }
}