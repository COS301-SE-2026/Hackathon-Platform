import {
  ChangeDetectorRef,
  Component,
  inject,
  OnInit,
  AfterViewInit,
  OnDestroy,
  ElementRef,
  ViewChild,
} from '@angular/core';
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
  styleUrls: ['./ide.component.scss'],
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
  private pasteDisposable?: monaco.IDisposable;
  private applyingRemoteEdit = false;
  private readonly fileVersions = new Map<string, number>();
  private readonly pendingOwnEdits = new Set<string>();
  private editTimer?: ReturnType<typeof setTimeout>;
  private typingTimer?: ReturnType<typeof setTimeout>;
  private typingCharacters = 0;
  private typingEdits = 0;
  private lastTypingTime?: number;
  private typingIntervalTotal = 0;
  private typingIntervalCount = 0;
  private deletionTimer?: ReturnType<typeof setTimeout>;
  private deletedCharacters = 0;
  private deletionEdits = 0;

  private readonly visibilityHandler = (): void => {
    if (document.hidden) {
      this.telService.recordEvent('TAB_HIDDEN');
    } else {
      this.telService.recordEvent('TAB_VISIBLE');
    }
  };

  private readonly focusGainedHandler = (): void => {
    this.telService.recordEvent('FOCUS_GAINED');
  };

  private readonly focusLossedHandler = (): void => {
    this.telService.recordEvent('FOCUS_LOST');
  };

  workspaceId = '';
  eventId = '';
  levelId = 0;
  startingIde = true;
  runtimeStatus = 'Starting...';
  output = 'Starting...';
  files: WorkspaceFileEntry[] = [];
  selectedFile: WorkspaceFileEntry | null = null;
  expandedDirs = new Set<string>();

  loadingFiles = false;
  savingFiles = false;
  runningCode = false;
  submittingCode = false;

  /** Files/folders that should currently be visible in the tree, in display order. */
  get visibleFiles(): (WorkspaceFileEntry & { depth: number })[] {
    const sorted = [...this.files].sort((a, b) => this.sortKey(a).localeCompare(this.sortKey(b)));
    return sorted
      .filter((file) => this.ancestorsExpanded(file.path))
      .map((file) => ({ ...file, depth: file.path.split('/').length - 1 }));
  }

  private sortKey(file: WorkspaceFileEntry): string {
    // Sort segment by segment so folders group with their children and come before sibling files.
    const parts = file.path.split('/');
    return parts
      .map((part, i) => {
        const isLast = i === parts.length - 1;
        const isFolder = !isLast || file.directory;
        return (isFolder ? '0' : '1') + part.toLowerCase();
      })
      .join('/');
  }

  private ancestorsExpanded(path: string): boolean {
    const parts = path.split('/');
    for (let i = 1; i < parts.length; i++) {
      if (!this.expandedDirs.has(parts.slice(0, i).join('/'))) {
        return false;
      }
    }
    return true;
  }

  toggleDir(file: WorkspaceFileEntry): void {
    if (this.expandedDirs.has(file.path)) {
      this.expandedDirs.delete(file.path);
    } else {
      this.expandedDirs.add(file.path);
    }
  }

  private languageFor(fileName: string): string {
    const ext = fileName.includes('.') ? fileName.split('.').pop()!.toLowerCase() : '';
    const map: Record<string, string> = {
      java: 'java',
      py: 'python',
      js: 'javascript',
      ts: 'typescript',
      json: 'json',
      xml: 'xml',
      html: 'html',
      css: 'css',
      md: 'markdown',
      c: 'c',
      h: 'c',
      cpp: 'cpp',
      cs: 'csharp',
      go: 'go',
      rs: 'rust',
      kt: 'kotlin',
      sql: 'sql',
      yml: 'yaml',
      yaml: 'yaml',
      sh: 'shell',
      txt: 'plaintext',
      gradle: 'groovy',
    };
    return map[ext] ?? 'plaintext';
  }

  /**
   * Apply text coming from another client (or the server) without wiping the local
   * cursor, selection or scroll position. Only the differing middle chunk is replaced.
   */
  private applyRemoteContent(content: string): void {
    const model = this.editor?.getModel();
    if (!this.editor || !model) {
      return;
    }

    const current = model.getValue();
    if (current === content) {
      return;
    }

    let start = 0;
    const minLen = Math.min(current.length, content.length);
    while (start < minLen && current.charCodeAt(start) === content.charCodeAt(start)) {
      start++;
    }

    let endCurrent = current.length;
    let endNew = content.length;
    while (
      endCurrent > start &&
      endNew > start &&
      current.charCodeAt(endCurrent - 1) === content.charCodeAt(endNew - 1)
    ) {
      endCurrent--;
      endNew--;
    }

    const startPos = model.getPositionAt(start);
    const endPos = model.getPositionAt(endCurrent);

    const selections = this.editor.getSelections();
    const scrollTop = this.editor.getScrollTop();
    const scrollLeft = this.editor.getScrollLeft();

    this.applyingRemoteEdit = true;
    try {
      model.applyEdits([
        {
          range: new monaco.Range(
            startPos.lineNumber,
            startPos.column,
            endPos.lineNumber,
            endPos.column,
          ),
          text: content.substring(start, endNew),
          forceMoveMarkers: false,
        },
      ]);
    } finally {
      this.applyingRemoteEdit = false;
    }

    if (selections) {
      this.editor.setSelections(selections);
    }
    this.editor.setScrollPosition({ scrollTop, scrollLeft });
  }

  ngOnInit(): void {
    // The IDE fills the viewport itself; stop the page behind it from scrolling too.
    document.documentElement.style.overflow = 'hidden';
    document.body.style.overflow = 'hidden';

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
        enabled: false,
      },
      fontSize: 14,
      lineHeight: 21,
      fontFamily: 'Consolas, "Courier New", monospace',
      fontLigatures: false,
      letterSpacing: 0,
      cursorStyle: 'line',
      cursorWidth: 2,
      cursorSmoothCaretAnimation: 'off',
      cursorBlinking: 'blink',
      padding: {
        top: 12,
        bottom: 12,
      },
      scrollBeyondLastLine: false,
      smoothScrolling: false,
      fixedOverflowWidgets: true,
      renderLineHighlight: 'line',
      wordWrap: 'off',
      tabSize: 4,
      scrollbar: {
        alwaysConsumeMouseWheel: false,
        verticalScrollbarSize: 12,
        horizontalScrollbarSize: 12,
      },
    });

    // If the editor measured its font before the font was ready, the caret is drawn in the
    // wrong place. Re-measure once fonts have loaded.
    document.fonts?.ready.then(() => monaco.editor.remeasureFonts());

    requestAnimationFrame(() => {
      this.editor?.layout();
      this.editor?.focus();
    });

    this.pasteDisposable = this.editor.onDidPaste((event) => {
      if (!this.selectedFile || !this.editor) {
        return;
      }

      const model = this.editor.getModel();

      if (!model) {
        return;
      }

      const pastedTxt = model.getValueInRange(event.range);
      const lines = pastedTxt.length === 0 ? 0 : pastedTxt.split(/\r\n|\r|\n/).length;

      this.telService.recordEvent('PASTE', {
        path: this.selectedFile.path,
        name: this.selectedFile.name,
        characters: pastedTxt.length,
        lines: lines,
      });
    });

    this.editorChangeDisposable = this.editor.onDidChangeModelContent((event) => {
      if (this.applyingRemoteEdit) {
        return;
      }

      if (!this.selectedFile || !this.editor) {
        return;
      }

      for (const change of event.changes) {
        const insertedCharacters = change.text.length;
        const removedCharacters = change.rangeLength;

        if (insertedCharacters > 0 && removedCharacters === 0 && insertedCharacters <= 2) {
          this.typingCharacters += insertedCharacters;
          this.typingEdits++;
          const now = performance.now();

          if (this.lastTypingTime !== undefined) {
            this.typingIntervalTotal += now - this.lastTypingTime;
            this.typingIntervalCount++;
          }

          this.lastTypingTime = now;
        }

        if (removedCharacters > 0) {
          this.deletedCharacters += removedCharacters;
          this.deletionEdits++;
        }
      }

      if (this.deletionTimer) {
        clearTimeout(this.deletionTimer);
      }

      this.deletionTimer = setTimeout(() => {
        if (this.deletedCharacters > 0 && this.selectedFile) {
          this.telService.recordEvent('DELETE', {
            path: this.selectedFile.path,
            name: this.selectedFile.name,
            characters: this.deletedCharacters,
            edits: this.deletionEdits,
          });
        }

        this.deletedCharacters = 0;
        this.deletionEdits = 0;
      }, 1000);

      if (this.typingTimer) {
        clearTimeout(this.typingTimer);
      }

      this.typingTimer = setTimeout(() => {
        if (this.typingCharacters > 0 && this.selectedFile) {
          const avgIntervalMs =
            this.typingIntervalCount > 0 ? this.typingIntervalTotal / this.typingIntervalCount : 0;

          this.telService.recordEvent('TYPING_BATCH', {
            path: this.selectedFile.path,
            name: this.selectedFile.name,
            characters: this.typingCharacters,
            edits: this.typingEdits,
            avgIntervalMs: Math.round(avgIntervalMs),
          });
        }

        this.typingCharacters = 0;
        this.typingEdits = 0;
        this.typingIntervalTotal = 0;
        this.typingIntervalCount = 0;
        this.lastTypingTime = undefined;
      }, 1000);

      if (this.editTimer) {
        clearTimeout(this.editTimer);
      }

      this.editTimer = setTimeout(() => {
        this.editTimer = undefined;
        this.sendCurrentEdit();
      }, 300);
    });

    document.addEventListener('visibilitychange', this.visibilityHandler);
    window.addEventListener('focus', this.focusGainedHandler);
    window.addEventListener('blur', this.focusLossedHandler);
  }

  ngOnDestroy(): void {
    document.documentElement.style.overflow = '';
    document.body.style.overflow = '';

    if (this.editTimer) {
      clearTimeout(this.editTimer);
    }

    void this.telService.endSession().catch((error) => {
      console.error('session could not be ended', error);
    });

    if (this.typingTimer) {
      clearTimeout(this.typingTimer);
    }

    if (this.deletionTimer) {
      clearTimeout(this.deletionTimer);
    }

    this.editorChangeDisposable?.dispose();
    this.collabSub?.unsubscribe();
    this.collabService.disconnect();
    this.editor?.dispose();
    this.pasteDisposable?.dispose();
    document.removeEventListener('visibilitychange', this.visibilityHandler);
    window.removeEventListener('focus', this.focusGainedHandler);
    window.removeEventListener('blur', this.focusLossedHandler);
  }

  private startIdeRuntime(): void {
    this.startingIde = true;
    this.runtimeStatus = 'Starting...';
    this.ideSessionService.startOrReuseSession(this.workspaceId).subscribe({
      next: (session) => {
        this.runtimeStatus = session.status;
        this.startingIde = false;
        this.output =
          'Workspace runtime started successfully. \n' + `Workspace: ${session.workspaceId}`;

        void this.telService.startSession(this.workspaceId).catch((error) => {
          console.error('session could not be started', error);
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
      },
    });
  }

  loadFiles(): void {
    this.loadingFiles = true;
    this.workService.listFiles(this.workspaceId).subscribe({
      next: (files) => {
        this.files = files;
        this.loadingFiles = false;
        // Open top-level folders by default so nested sources are visible right away.
        files
          .filter((f) => f.directory && !f.path.includes('/'))
          .forEach((f) => this.expandedDirs.add(f.path));
        this.change.markForCheck();
      },

      error: () => {
        this.loadingFiles = false;
        this.toast.error('File Error', 'Workspace files could not be loaded');
        this.change.markForCheck();
      },
    });
  }

  openFile(file: WorkspaceFileEntry): void {
    if (file.directory) {
      this.toggleDir(file);
      return;
    }

    this.editor?.updateOptions({
      readOnly: true,
    });

    this.workService.readFile(this.workspaceId, file.path).subscribe({
      next: (res) => {
        this.selectedFile = file;

        this.telService.recordEvent('FILE_OPENED', {
          path: file.path,
          name: file.name,
        });

        if (this.editor) {
          this.applyingRemoteEdit = true;
          const model = this.editor.getModel();
          if (model) {
            monaco.editor.setModelLanguage(model, this.languageFor(file.name));
          }
          this.editor.setValue(res.content);
          this.editor.setPosition({
            lineNumber: 1,
            column: 1,
          });
          this.editor.setScrollPosition({ scrollTop: 0, scrollLeft: 0 });
          this.applyingRemoteEdit = false;

          requestAnimationFrame(() => {
            this.editor?.layout();
            this.editor?.focus();
          });
        }

        this.collabService.getVersion(this.workspaceId, file.path).subscribe({
          next: (versionRes) => {
            const knownVersion = this.fileVersions.get(file.path) ?? 0;
            this.fileVersions.set(file.path, Math.max(knownVersion, versionRes.version));
            this.editor?.updateOptions({
              readOnly: false,
            });
            this.change.markForCheck();
          },
        });
        this.change.markForCheck();
      },

      error: () => {
        this.editor?.updateOptions({
          readOnly: false,
        });
        this.toast.error('File Error', 'Workspace files could not be opened');
        this.change.markForCheck();
      },
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
      },
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
          this.output = 'Could not save current file';
          this.toast.error('Run Error', 'Current file could not save');
          this.change.markForCheck();
        },
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
    this.output = 'Submitting...';

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
          this.output = 'Could not save current file before submitting';
          this.toast.error('Submission Error', 'Current file could not be saved');
          this.change.markForCheck();
        },
      });

      return;
    }

    this.executeSubmit();
  }

  private executeSubmit(): void {
    this.telService.recordEvent('SUBMIT', {
      path: this.selectedFile?.path ?? null,
      name: this.selectedFile?.name ?? null,
    });
    this.workService.submitWorkspace(this.workspaceId).subscribe({
      next: (res) => {
        this.submittingCode = false;
        this.output = `Submission queued successfully. \n Submission Id: ${res.submissionId} \n Status: ${res.status}`;
        this.toast.success('Submitted', 'You have beed placed in the queue');
        this.change.markForCheck();
      },
      error: (err) => {
        this.submittingCode = false;
        const message = err?.error?.message ?? 'Workspace could not be submitted';
        this.output = message;
        this.toast.error('Submission Error', message);
        this.change.markForCheck();
      },
    });
  }

  private executeRun(): void {
    this.telService.recordEvent('RUN', {
      path: this.selectedFile?.path ?? null,
      name: this.selectedFile?.name ?? null,
    });
    this.workService.runWorkspace(this.workspaceId).subscribe({
      next: (res) => {
        this.runningCode = false;

        this.telService.recordEvent('RUN_RESULT', {
          path: this.selectedFile?.path ?? null,
          name: this.selectedFile?.name ?? null,
          success: res.success,
          result: res.success ? 'SUCCESS' : 'CODE_ERROR',
        });

        if (res.success) {
          this.output = res.output;
        } else {
          this.output = res.error;
        }
        this.change.markForCheck();
      },
      error: () => {
        this.runningCode = false;

        this.telService.recordEvent('RUN_RESULT', {
          path: this.selectedFile?.path ?? null,
          name: this.selectedFile?.name ?? null,
          success: false,
          result: 'REQUEST_ERROR',
        });

        this.output = 'Workspace could not be executed.';
        this.toast.error('Run Error', 'Workspace could not execute');
        this.change.markForCheck();
      },
    });
  }

  private connectCollaboration(): void {
    const token = this.authService.getToken();
    if (!token) {
      this.toast.error('Collaboration Error', 'Authentication token cant be found');
      return;
    }

    this.collabService.connect(this.workspaceId, token);
    this.collabSub = this.collabService.edits$.subscribe((edit) => {
      this.fileVersions.set(edit.path, edit.version);

      const myUserId = this.authService.getUser()?.userId;
      if (edit.editedByUserId === myUserId || this.pendingOwnEdits.has(edit.content)) {
        this.pendingOwnEdits.delete(edit.content);
        return;
      }

      // Don't overwrite what the user is typing right now; the next broadcast will catch up.
      if (this.editTimer) {
        return;
      }

      if (this.selectedFile?.path === edit.path) {
        this.applyRemoteContent(edit.content);
      }
    });
  }

  private sendCurrentEdit(): void {
    if (!this.selectedFile || !this.editor) {
      return;
    }

    const path = this.selectedFile.path;
    const currentVersion = this.fileVersions.get(path) ?? 0;
    const content = this.editor.getValue();
    this.pendingOwnEdits.add(content);
    this.collabService.sendEdit(this.workspaceId, {
      path: path,
      content: this.editor.getValue(),
      baseVersion: currentVersion,
    });
  }

  backToLevel(): void {
    this.router.navigate(['/participant/events', this.eventId], {
      queryParams: {
        tab: 'submissions',
        subtab: this.levelId.toString(),
      },
    });
  }
}
