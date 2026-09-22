import { ChangeDetectorRef, Component, inject, OnInit, AfterViewInit, OnDestroy, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { ButtonComponent } from '../../shared/components/button/button.component';
import { ToastService } from '../../shared/components/toast/toast.service';
import { IdeSessionService } from '../../services/ide-session.service';
import { WorkspaceFileService, WorkspaceFileEntry } from '../../services/workspace-file.service';
import * as monaco from 'monaco-editor';
import { Subscription } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { WorkspaceCollaborationService } from '../../services/workspace-collaboration.service';
import { WorkspaceTelemetryService } from '../../services/workspace-telemetry.service';

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
    private readonly authService = inject(AuthService);
    private readonly collabService = inject(WorkspaceCollaborationService);
    private readonly telService = inject(WorkspaceTelemetryService);
    private readonly change = inject(ChangeDetectorRef);

    @ViewChild('editorContainer')
    editorContainer!: ElementRef<HTMLDivElement>;
    private editor?: monaco.editor.IStandaloneCodeEditor;
    private collabSub?: Subscription;
    private editorChangeDisposable?: monaco.IDisposable;
    private applyingRemoteEdit = false;
    private readonly fileVersions = new Map<string, number>();
    private editTimer?: ReturnType<typeof setTimeout>;

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

        this.editorChangeDisposable = this.editor.onDidChangeModelContent(() => {
            if (this.applyingRemoteEdit) {
                return;
            }

            if (!this.selectedFile || !this.editor) {
                return;
            }

            if (this.editTimer) {
                clearTimeout(this.editTimer);
            }

            this.editTimer = setTimeout(() => {
                this.sendCurrentEdit();
            }, 300);
        })

    }

    ngOnDestroy(): void {
        if (this.editTimer) {
            clearTimeout(this.editTimer);
        }

        void this.telService.endSession().catch(error => {
            console.error('session could not be ended', error);
        });

        this.editorChangeDisposable?.dispose();
        this.collabSub?.unsubscribe();
        this.collabService.disconnect();
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

                void this.telService.startSession(this.workspaceId).catch(error => {
                    console.error('session could not be started', error)
                });

                this.loadFiles();
                this.connectCollaboration();
                this.change.markForCheck();
            },

            error: () => {
                this.runtimeStatus = 'ERROR';
                this.startingIde = false;
                this.output = 'workspace could not be started';
                this.toast.error('IDE Error', 'Workspace could not be started');
                this.change.markForCheck();
            }
        });
    }

    loadFiles(): void {
        this.loadingFiles = true;
        this.workService.listFiles(this.workspaceId).subscribe({
            next: files => {
                this.files = files;
                this.loadingFiles = false;
                this.change.markForCheck();
            },

            error: () => {
                this.loadingFiles = false;
                this.toast.error('File Error', 'Workspace files could not be loaded');
                this.change.markForCheck();
            }
        });
    }

    openFile(file: WorkspaceFileEntry): void {
        if (file.directory) {
            return;
        }

        this.editor?.updateOptions({
            readOnly: true
        });

        this.workService.readFile(this.workspaceId, file.path).subscribe({
            next: res => {
                this.selectedFile = file;

                if (this.editor) {
                    this.applyingRemoteEdit = true;
                    this.editor.setValue(res.content);
                    this.applyingRemoteEdit = false;
                }

                this.collabService.getVersion(this.workspaceId, file.path).subscribe({
                    next: versionRes => {
                        const knownVersion = this.fileVersions.get(file.path) ?? 0;
                        this.fileVersions.set(file.path, Math.max(knownVersion, versionRes.version));
                        this.editor?.updateOptions({
                            readOnly: false
                        });
                        this.change.markForCheck();
                    }
                })
                this.change.markForCheck();
            },

            error: () => {
                this.editor?.updateOptions({
                    readOnly: false
                });
                this.toast.error('File Error', 'Workspace files could not be opened');
                this.change.markForCheck();
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
                this.change.markForCheck();
            }, 

            error: () => {
                this.savingFiles = false;
                this.toast.error('File Error', 'Workspace files could not be saved');
                this.change.markForCheck();
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
                    this.change.markForCheck();
                },
                error: () => {
                    this.savingFiles = false;
                    this.runningCode = false;
                    this.output = 'Could not save current file'
                    this.toast.error('Run Error', 'Current file could not save');
                    this.change.markForCheck();
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
                    this.change.markForCheck();
                },
                error: () => {
                    this.savingFiles = false;
                    this.submittingCode = false;
                    this.output = "Could not save current file before submitting";
                    this.toast.error('Submission Error', 'Current file could not be saved');
                    this.change.markForCheck();
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
                this.change.markForCheck();
            },
            error: err => {
                this.submittingCode = false;
                const message = err?.error?.message ?? 'Workspace could not be submitted';
                this.output = message;
                this.toast.error('Submission Error', message);
                this.change.markForCheck();
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
                this.change.markForCheck();
            },
            error: () => {
                this.runningCode = false;
                this.output = 'Workspace could not be executed.'
                this.toast.error('Run Error', 'Workspace could not execute');
                this.change.markForCheck();
            }
        });
    }

    private connectCollaboration(): void {
        const token = this.authService.getToken();
        if (!token) {
            this.toast.error('Collaboration Error', 'Authentication token cant be found');
            return;
        }

        this.collabService.connect(this.workspaceId, token);
        this.collabSub = this.collabService.edits$.subscribe(edit => {
            this.fileVersions.set(edit.path, edit.version);
            if (this.selectedFile?.path === edit.path && this.editor && this.editor.getValue() !== edit.content) {
                this.applyingRemoteEdit = true;
                this.editor.setValue(edit.content);
                this.applyingRemoteEdit = false;
            }
        });
    }

    private sendCurrentEdit(): void {
        if (!this.selectedFile || !this.editor) {
            return;
        }

        const path = this.selectedFile.path;
        const currentVersion = this.fileVersions.get(path) ?? 0;
        this.collabService.sendEdit(this.workspaceId, {
            path: path,
            content: this.editor.getValue(),
            baseVersion: currentVersion
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