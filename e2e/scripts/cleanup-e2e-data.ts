import { ApiClient } from '../utils/api-client';

async function main() {

    const api = await ApiClient.loginAsAdmin();

    const all = await api.listHackathons();
    const testHackathons = all.filter((h) => h.name.startsWith('E2E '));

    console.log(`Found ${testHackathons.length} E2E test hackathon(s) out of ${all.length} total.`);

    const stuck: string[] = [];
    const cleaned: string[] = [];

    for (const h of testHackathons) {


        // Clear levels first for the hackathon delete
        const levels = await api.listLevels(h.hackathonId);
        await api.deleteLevels(h.hackathonId, levels.map((l) => l.id));

        const before = (await api.listHackathons()).some((x) => x.hackathonId === h.hackathonId);
        await api.deleteHackathon(h.hackathonId);
        const after = (await api.listHackathons()).some((x) => x.hackathonId === h.hackathonId);

        if (before && !after) {

            cleaned.push(h.name);
        } else if (!before) {

            cleaned.push(h.name); 
        } else {

            stuck.push(h.name);

        }
    }

    console.log(`\nCleaned up: ${cleaned.length}`);
    if (stuck.length) {
        console.log(`\nStuck (has events attached, can't be deleted until the backend fix lands): ${stuck.length}`);
        stuck.forEach((name) => console.log(`  - ${name}`));


    }

    await api.dispose();
}

main().catch((err) => {
    console.error(err);
    process.exit(1);
    
});