import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

interface Submission {
  id: string;
  level: string;
  score: number;
  status: 'Scored' | 'Error' | 'Pending';
  runtime: string;
  submitted: string;
  errorLog?: string;
}

@Component({
  selector: 'app-submissionhistory',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './submissionhistory.component.html',
  styleUrls: ['./submissionhistory.component.scss']
}
)
export class SubmissionHistoryComponent {
  levelFilter: string = '';
  statusFilter: string = '';
  selectedSubmission: Submission | null = null;


  submissions: Submission[] = [
    {
      id: '#32',
      level: 'Level3',
      score: 94210,
      status: 'Scored',
      runtime: '1.8s',
      submitted: '2m ago',
    },
    {
      id: '#31',
      level: 'Level2',
      score: 45200,
      status: 'Scored',
      runtime: '1.2s',
      submitted: '15m ago',
    },
    {
      id: '#30',
      level: 'Level3',
      score: 0,
      status: 'Error',
      runtime: '0.4s',
      submitted: '10m ago',
      errorLog: 'RuntimeError: index out of bounds at line 42\nStack trace:\n  at solve() main.py:42\n  at main() main.py:88'
    },
    {
      id: '#29',
      level: 'Level1',
      score: 100,
      status: 'Scored',
      runtime: '0.8s',
      submitted: '1h ago',
    },
    {
      id: '#28',
      level: 'Level1',
      score: 85,
      status: 'Scored',
      runtime: '0.9s',
      submitted: '2h ago',
    }
  ];

  get filteredSubmissions(): Submission[] {
    return this.submissions.filter(s => {
      const matchLevel = !this.levelFilter || s.level === this.levelFilter;
      const matchStatus = !this.statusFilter || s.status === this.statusFilter;
      return matchLevel && matchStatus;
    }
  );
  }

  viewLogs(sub: Submission): void {
    this.selectedSubmission = this.selectedSubmission?.id === sub.id ? null : sub;
  }

  downloadCode(sub: Submission): void {
    console.log('Downloading code for', sub.id);
     alert(`Downloading code for ${sub.id}`);
  }
}