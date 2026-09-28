import { test, expect } from './fixtures/auth.fixture';
import {AdminTeamPage} from './pages/admin-team.page'

test.describe('Admin > Teams', () =>{
    test('full lifecycle: create -> add member -> search -> remove member -> delete', async({
        adminPage,
        eventId,
        uniqueSuffix,
    }) => {
        const eventName = `E2E Event ${uniqueSuffix}`;
        const teams = new AdminTeamPage(adminPage,eventId,eventName);
        const teamName = `Alpha ${uniqueSuffix}`;
        const firstMember = 'Alice';
        const secondMember = 'Bob';

        await teams.goto();

        await teams.createTeamBtn.click();
        await teams.fillTeamName(teamName);
        await teams.addPendingMember(firstMember, 'alice@e2e.test');
        await teams.confirmCreateTeam();

        const card = teams.teamCardByName(teamName);
        await expect(card).toBeVisible();
        await expect(card.locator('.team-subline')).toContainText(/1 member/);

        await teams.expandTeam(teamName);
        await teams.addMemberToTeam(teamName, secondMember, 'bob@e2e.test');
        await expect(card.locator('.member-card')).toHaveCount(2);
        await expect(card.locator('.team-subline')).toContainText(/2 members/);

        await teams.searchInput.fill(teamName);
        await expect(teams.teamCards).toHaveCount(1);
        await expect(teams.teamCardByName(teamName)).toBeVisible();

        await teams.searchInput.fill(teamName);
        await teams.removeMemberFromTeam(teamName, secondMember);
        await expect(card.locator('.member-card')).toHaveCount(1);
        await expect(card.locator('.member-card',{hasText: secondMember})).toHaveCount(0);

        await teams.deleteTeam(teamName);
        await expect(teams.teamCardByName(teamName)).toHaveCount(0);
    });

    test('search with no match shows empty state', async ({ adminPage, eventId,uniqueSuffix})=>{
        const eventName = `E2E Event ${uniqueSuffix}`
        const teams = new AdminTeamPage(adminPage,eventId,eventName);
        await teams.goto();

        await teams.searchInput.fill('zzz-no-suc-team-xyz');
        await expect(adminPage.locator('.empty-state')).toContainText(/no teams found/i);
    });

    test('create modal validation blocks empty team name',async({adminPage, eventId,uniqueSuffix}) =>{
        const eventName = `E2E Event ${uniqueSuffix}`
        const teams = new AdminTeamPage(adminPage,eventId,eventName);
        await teams.goto();

        await teams.createTeamBtn.click();

        await expect(
            adminPage.getByRole('dialog').getByRole('button', {name: /^create team$/i}),

        ).toBeDisabled();

        await teams.fillTeamName('Valid Name');
        await expect(
            adminPage.getByRole('dialog').getByRole('button',{name: /^create team$/i}),

        ).toBeEnabled();
    });

    test('deleting a tea,, removes it from the list', async({adminPage,eventId,uniqueSuffix})=>{
        const eventName = `E2E Event ${uniqueSuffix}`
        const teams = new AdminTeamPage(adminPage, eventId,eventName);
        const teamName = `DeleteMe ${uniqueSuffix}`;

        await teams.goto();
        await teams.createTeamBtn.click();
        await teams.fillTeamName(teamName);
        await teams.confirmCreateTeam();
        await expect(teams.teamCardByName(teamName)).toBeVisible();

        await teams.deleteTeam(teamName);
        await expect(teams.teamCardByName(teamName)).toHaveCount(0);
    });
});