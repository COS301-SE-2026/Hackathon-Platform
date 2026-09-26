import { ChangeDetectorRef,Component,inject,OnInit,Input } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";

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

    @Input() hackathonId = '';
    @Input() eventId = '';
    isLoading = false;
    errorMessage = '';
    searchTerm = '';
    onlyShowRequirements = false;

    teamGroups: TeamDietaryGroup[] = [];

    
    ngOnInit(): void {
        this.hackathonId = this.hackathonId ||  this.route.snapshot.paramMap.get('hackathonId') || '';
        this.eventId = this.eventId ||  this.route.snapshot.paramMap.get('eventId') || '';

        if (!this.eventId){
            
            this.errorMessage = 'No event ID provided.';
            return;
        }
        this.isLoading = false;
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

}