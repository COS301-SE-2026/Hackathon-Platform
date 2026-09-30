import { ChangeDetectorRef,Component,inject,OnInit,Input } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { EventService, EventParticipantResponse } from '../../../services/event.service';

type DietaryTagKind = 'allergy' | 'intolerance' | 'preference';

interface DietaryTag{
    label: string;
    kind: DietaryTagKind;
}

interface DietaryEntry{
    participantId: string;
    name: string;
    initial: string;
    tags: DietaryTag[];
}

interface TeamDietaryGroup{
    teamId: string;
    teamName: string;
    members: DietaryEntry[];
}

@Component({
    selector: 'app-dietary-requirements',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './dietary-requirements.component.html',
    styleUrls: ['./dietary-requirements.component.scss']
})

export class DietaryRequirementsComponent implements OnInit {
    private readonly change = inject(ChangeDetectorRef);
    private readonly route = inject(ActivatedRoute);
    private readonly eventService = inject(EventService);

    @Input() hackathonId = '';
    @Input() eventId = '';
    isLoading = false;
    errorMessage = '';
    searchTerm = '';
    onlyShowRequirements = false;

    teamGroups: TeamDietaryGroup[] = [];

    
    ngOnInit(): void {
    this.hackathonId =
        this.hackathonId || this.route.snapshot.paramMap.get('hackathonId') || '';

    this.eventId =
        this.eventId || this.route.snapshot.paramMap.get('eventId') || '';

    if (!this.eventId) {
        this.errorMessage = 'No event ID provided.';
        return;
    }

    this.loadParticipants();
}

    get totalParticipants(): number{
        return this.teamGroups.reduce((sum, group) => sum + group.members.length,0);
    }
  
    get participantsWithRequirements(): number{
        return this.teamGroups.reduce(
            (sum, group) => sum + group.members.filter(member =>member.tags.length > 0).length, 0);
        
    }
    get teamCount(): number {
        return this.teamGroups.length;
    }

    toggleOnlyShowRequirements(): void{
        this.onlyShowRequirements = !this.onlyShowRequirements;
        this.change.markForCheck();
    }

    get filteredGroups(): TeamDietaryGroup[] {
        const term = this.searchTerm.trim().toLowerCase();
        return this.teamGroups.map(group => {
            const members = group.members.filter(member => {
                const matchesSearch =
                !term || 
                member.name.toLowerCase().includes(term) || 
                group.teamName.toLowerCase().includes(term) ||
                member.tags.some(tag => tag.label.toLowerCase().includes(term));
                const matchesFilter = !this.onlyShowRequirements || member.tags.length > 0;
                return matchesSearch && matchesFilter;
            });
            return {...group, members};
        })
        .filter(group => group.members.length > 0);
    }


private loadParticipants(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.eventService.getEventParticipants(this.eventId).subscribe({
        next: (participants) => {
            this.teamGroups = this.buildTeamGroups(participants);
            this.isLoading = false;
            this.change.markForCheck();
        },
        error: (error) => {
            console.error('Failed to load participants:', error);
            this.errorMessage = 'Failed to load participant dietary requirements.';
            this.isLoading = false;
            this.change.markForCheck();
        }
    });
}

private buildTeamGroups(
    participants: EventParticipantResponse[]
): TeamDietaryGroup[] {

    const groups = new Map<string, TeamDietaryGroup>();

    participants.forEach(participant => {

        const teamId = participant.teamId ?? 'no-team';
        const teamName = participant.teamName ?? 'No Team';

        if (!groups.has(teamId)) {
            groups.set(teamId, {
                teamId,
                teamName,
                members: []
            });
        }

        const tags: DietaryTag[] = [];

        if (participant.allergies?.trim()) {
            tags.push({
                label: participant.allergies.trim(),
                kind: 'allergy'
            });
        }

        if (participant.dietaryReq?.trim()) {
            tags.push({
                label: participant.dietaryReq.trim(),
                kind: 'preference'
            });
        }

        groups.get(teamId)!.members.push({
            participantId: participant.userId,
            name: participant.fullName,
            initial: participant.fullName.charAt(0).toUpperCase(),
            tags
        });
    });

    return Array.from(groups.values());
}

}