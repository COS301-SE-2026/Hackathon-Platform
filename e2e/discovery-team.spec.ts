import { expect, test } from '@playwright/test'
import { DiscoveryPage } from './pages/discovery.page'
import { TeamPage } from './pages/team.page'

const TEST_EVENT_ID = 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13';

test.describe('Discovery and Team Flow', () => {
    //test.describe.configure({ mode: 'serial' });

    let discoveryPage: DiscoveryPage;
    let teamPage: TeamPage;

    test.beforeEach(async ({ page }) => {
        discoveryPage = new DiscoveryPage(page);
        teamPage = new TeamPage(page);
    });

    test('should show participant home and browse page', async () => {
        await discoveryPage.goto();
        await discoveryPage.expectBrowsePageVisible();
        // await discoveryPage.openFirstEvent();
        // await discoveryPage.expectEventDetailsTabsVisible();
    });

    test('should open events details', async () => {
        await discoveryPage.gotoEventDetails(TEST_EVENT_ID);
        await discoveryPage.expectEventDetailsTabsVisible();
    });

    test('should show rules tab on event details', async () => {
        await discoveryPage.gotoEventDetails(TEST_EVENT_ID);
        await discoveryPage.expectEventDetailsTabsVisible();
        await discoveryPage.navigateToRules();
        await discoveryPage.expectRulesVisible();
    });

    test('should show Overview tab on event details', async () => {
        await discoveryPage.gotoEventDetails(TEST_EVENT_ID);
        await discoveryPage.expectEventDetailsTabsVisible();
        await discoveryPage.navigateToOverview();
        await discoveryPage.expectOverviewVisible();
    });

    test('should show My Team tab on event details', async () => {
        await discoveryPage.gotoEventDetails(TEST_EVENT_ID);
        await discoveryPage.expectEventDetailsTabsVisible();
        await teamPage.goToMyTeam();
        await teamPage.expectMyTeamTabVisible();
    });

    test('should allow participant to start create team flow when available', async () => {
        await discoveryPage.gotoEventDetails(TEST_EVENT_ID);
        await teamPage.startCreateTeamFlow();
        await teamPage.expectCreateTeamFormVisible();
    });

    test('should allow participant to start join team flow when available', async () => {
        await discoveryPage.gotoEventDetails(TEST_EVENT_ID);
        await teamPage.startJoinTeamFlow();
        await teamPage.expectJoinTeamFlowVisible();
    });
})