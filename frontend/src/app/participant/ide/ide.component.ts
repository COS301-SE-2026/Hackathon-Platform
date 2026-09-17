import { Component, inject, OnInit, AfterViewInit, OnDestroy, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { ButtonComponent } from '../../shared/components/button/button.component';
import { ToastService } from '../../shared/components/toast/toast.service';
import { IdeSessionService } from '../../services/ide-session.service';
import { WorkspaceFileService, WorkspaceFileEntry } from '../../services/workspace-file.service';
import * as monaco from 'monaco-editor';

@Component({
  selector: 'app-ide',
  standalone: true,
  imports: [CommonModule, ButtonComponent],
  templateUrl: './ide.component.html',
  styleUrls: ['./ide.component.scss']
})
export class IdeComponent implements OnInit, AfterViewInit, OnDestroy {
    private readonly route = inject(ActivatedRoute);
    private readonly router = inject(Router);
    private readonly ideSessionService = inject(IdeSessionService);
    private readonly toast = inject(ToastService);
    private readonly workService = inject(WorkspaceFileService);

    @ViewChild('editorContainer')
    editorContainer!: ElementRef<HTMLDivElement>;
    private editor?: monaco.editor.IStandaloneCodeEditor;

    workspaceId = '';
    eventId = '';
    levelId = 0;
    startingIde = true;
    runtimeStatus = 'Starting...';
    output = 'Starting...';
    files: WorkspaceFileEntry[] = [];
    selectedFile: WorkspaceFileEntry | null = null;

    loadingFiles = false;
    savingFiles = false;
    runningCode = false;
    submittingCode = false;

    ngOnInit(): void {
        this.workspaceId = this.route.snapshot.paramMap.get('workspaceId') ?? '';
        this.eventId = this.route.snapshot.paramMap.get('eventId') ?? '';
        this.levelId = Number(this.route.snapshot.paramMap.get('levelId'));

        if (!this.workspaceId || !this.eventId || !this.levelId) {
            this.runtimeStatus = 'Invalid workspace';
            this.output = 'workspace could not be identified';
            this.startingIde = false;
            return;
        }

        this.startIdeRuntime();
    }

    ngAfterViewInit(): void {
        this.editor = monaco.editor.create(this.editorContainer.nativeElement, {
            value: '',
            language: 'java',
            theme: 'vs-dark',
            automaticLayout: true,
            readOnly: false,
            minimap: {
                enabled: false
            },
            fontSize: 14
        });
    }

    ngOnDestroy(): void {
        this.editor?.dispose();
    }

    private startIdeRuntime(): void {
        this.startingIde = true;
        this.runtimeStatus = "Starting...";
        this.ideSessionService.startOrReuseSession(this.workspaceId).subscribe({
            next: session => {
                this.runtimeStatus = session.status;
                this.startingIde = false;
                this.output = 'Workspace runtime started successfully. \n' + `Workspace: ${session.workspaceId}`;
                this.loadFiles();
            },

            error: () => {
                this.runtimeStatus = 'ERROR';
                this.startingIde = false;
                this.output = 'workspace could not be started';
                this.toast.error('IDE Error', 'Workspace could not be started');
            }
        });
    }

    loadFiles(): void {
        this.loadingFiles = true;
        this.workService.listFiles(this.workspaceId).subscribe({
            next: files => {
                this.files = files;
                this.loadingFiles = false;
            },

            error: () => {
                this.loadingFiles = false;
                this.toast.error('File Error', 'Workspace files could not be loaded');
            }
        });
    }

    openFile(file: WorkspaceFileEntry): void {
        if (file.directory) {
            return;
        }

        this.workService.readFile(this.workspaceId, file.path).subscribe({
            next: res => {
                this.selectedFile = file;
                this.editor?.setValue(res.content);
            },

            error: () => {
                this.toast.error('File Error', 'Workspace files could not be opened');
            }
        });
    }

    saveCurrentFile(): void {
        if (!this.selectedFile || !this.editor) {
            return;
        }

        const content = this.editor.getValue();

        this.savingFiles = true;
        this.workService.saveFile(this.workspaceId, this.selectedFile.path, content).subscribe({
            next: () => {
                this.savingFiles = false;
                this.toast.success('Saved', `${this.selectedFile?.name} saved`);
            }, 

            error: () => {
                this.savingFiles = false;
                this.toast.error('File Error', 'Workspace files could not be saved');
            }
        });
    }

    runCode(): void {
        if (this.runningCode) {
            return;
        }

        this.runningCode = true;
        this.output = 'Running...';

        if (this.selectedFile && this.editor) {
            const content = this.editor.getValue();
            this.savingFiles = true;

            this.workService.saveFile(this.workspaceId, this.selectedFile.path, content).subscribe({
                next: () => {
                    this.savingFiles = false;
                    this.executeRun();
                },
                error: () => {
                    this.savingFiles = false;
                    this.runningCode = false;
                    this.output = 'Could not save current file'
                    this.toast.error('Run Error', 'Current file could not save');
                }
            });

            return;
        }

        this.executeRun();
    }

    submitCode(): void {
        if (this.submittingCode) {
            return;
        }

        this.submittingCode = true;
        this.output = "Submitting...";

        if (this.selectedFile && this.editor) {
            const content = this.editor.getValue();
            this.savingFiles = true;
            this.workService.saveFile(this.workspaceId, this.selectedFile.path, content).subscribe({
                next: () => {
                    this.savingFiles = false;
                    this.executeSubmit();
                },
                error: () => {
                    this.savingFiles = false;
                    this.submittingCode = false;
                    this.output = "Could not save current file before submitting";
                    this.toast.error('Submission Error', 'Current file could not be saved');
                }
            });

            return;
        }

        this.executeSubmit();
    }

    private executeSubmit(): void {
        this.workService.submitWorkspace(this.workspaceId).subscribe({
            next: res => {
                this.submittingCode = false;
                this.output = `Submission queued successfully. \n Submission Id: ${res.submissionId} \n Status: ${res.status}`;
                this.toast.success('Submitted', 'You have beed placed in the queue');
            },
            error: err => {
                this.submittingCode = false;
                const message = err?.error?.message ?? 'Workspace could not be submitted';
                this.output = message;
                this.toast.error('Submission Error', message);
            }
        });
    }

    private executeRun(): void {
        this.workService.runWorkspace(this.workspaceId).subscribe({
            next: res => {
                this.runningCode = false;
                if (res.success) {
                    this.output = res.output;
                } else {
                    this.output = res.error;
                }
            },
            error: () => {
                this.runningCode = false;
                this.output = 'Workspace could not be executed.'
                this.toast.error('Run Error', 'Workspace could not execute');
            }
        });
    }

    backToLevel(): void {
        this.router.navigate(['/participant/events', this.eventId], {
            queryParams: {
                tab: 'submissions',
                subtab: this.levelId.toString()
            }
        });
    }
}