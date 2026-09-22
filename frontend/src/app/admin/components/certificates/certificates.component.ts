import { Component, OnInit, onDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { Observable } from 'rxjs';
import { EventService, EventResponse } from '../../../services/event.service.js';
import { CertificateService, CertificateTemplateResponse, CertificateLayout, CertificateElement, CertificateElementType, CertificateField, CertificateGenerationRunResponse, CertificateIssuedResponse, GenerationScope } from '../../../services/certificate.service';

const CANVAS_WIDTH = 842;
const CANVAS_HEIGHT = 595;


const FIELD_OPTIONS: { value: CertificateField; label: string }[] = [
  { value: 'participantName', label: 'Participant Name' },
  { value: 'teamName', label: 'Team Name' },
  { value: 'eventName', label: 'Event Name' },
  { value: 'rank', label: 'Rank (e.g. "1st")' },
  { value: 'certificateType', label: 'Certificate Type' },
  { value: 'date', label: 'Date Issued' },
];

const CERT_TYPES = ['WINNER', 'RUNNER_UP', 'THIRD_PLACE', 'PARTICIPATION'];

function blankLayout(): CertificateLayout {
  return {
    pageSize: 'A4-landscape',
    elements: [
      {
        type: 'TEXT',
        field: null,
        staticText: 'CERTIFICATE OF ACHIEVEMENT',
        x: CANVAS_WIDTH/2,
        y: 100,
        font: 'helvetica-bold',
        fontSize: 26,
        color: '#1a2340',
        align: 'center',
        visibleForTypes: null,
      },
      {
        type: 'TEXT',
        field: 'participantName',
        x: CANVAS_WIDTH/2,
        y: 230,
        font: 'times-bold',
        fontSize: 34,
        color: '#b6942f',
        align: 'center',
        visibleForTypes: null,
      },
      {
        type: 'TEXT',
        field: 'eventName',
        x: CANVAS_WIDTH_/2,
        y: 300,
        font: 'helvetica',
        fontSize: 16,
        color: '#4a4f57',
        align: 'center',
        visibleForAllTypes: null,
      },
      {
        type: 'TEXT',
        field: 'rank',
        staticText: undefined,
        x: CANVAS_WIDTH/2,
        y: 340,
        fontSize: 15,
        color: '#1a2340',
        align: 'center',
        visibleForTypes: 'WINNER,RUNNER_UP,THIRD_PLACE',
      },
      {
        type: 'TEXT',
        field: 'date',
        x: 120,
        y: 540,
        font: 'helvetica',
        fontSize: 11,
        color: '#4a4f57',
        align: 'left',
        visibleForTypes: null,
      },
      {
        type: 'QR',
        x: CANVAS_WIDTH-170,
        y: 480,
        fontSize: 18, //This must NEVER be changed, if you do CHANGE it in the backend
        visibleForTypes: null,
      },
    ],
  };
}

@Component({
  selector: 'app-certificates',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './certificates.component.html',
  styleUrls: ['./certificates.component.scss'],
})
export class CertificatesComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly eventService = inject(EventService);
  private readonly certificateService = inject(CertificateService);
  readonly canvasWidth = CANVAS_WIDTH;
  readonly canvasHeight = CANVAS_HEIGHT;
  readonly fieldOptions = FIELD_OPTIONS;
  readonly certTypes = CERT_TYPES;
  eventId = '';
  event: EventResponse | null = null;
  isLoading = true;
  errorMessage = '';
  successMessage = '';
  templates: CertificateTemplateResponse[] = [];
  selectedTemplateId: string | null = null;
  templateName = 'Untitled Certificate';
  layout: CertificateLayout = blankLayout();
  backgroundUrl: string | null = null;
  backgroundFile: File | null = null;
  isSaving = false;
  assetUrlByKey: Record<string, string> = {};
  isUploadingAsset = false;
  selectedElementIndex: number | null = null;
  private dragging = false;
  private dragElementIndex = -1;
  private dragOffsetX = 0;
  private dragOffsetY = 0;
  generationScope: GenerationScope = 'ALL_PARTICIPANTS';
  topN = 10;
  activeRun: CertificateGenerationRunResponse | null = null;
  runs: CertificateGenerationRunResponse[] = [];
  issued: CertificateIssuedResponse[] = [];
  isGenerating = false;
  private pollHandle: ReturnType<typeof setInterval> | null = null;

  get selectedElement(): CertificateElement | null {
    return this.selectedElementIndex === null ? null : this.layout.elements[this.selectedElementIndex];
  }

  ngOnInit(): void {
    this.eventId = this.route.snapshot.paramMap.get('eventId') || '';
    if (!this.eventId) {
      this.errorMessage = 'No event id provided';
      this.isLoading = true;
      return;
    }
    this.loadEvent();
    this.loadTemplates();
    this.loadRuns();
    this.loadIssues();
  }

  ngOnDestroy(): void {
    if(this.pollHandle) {
      clearInterval(this.pollHandle);
    }
  }

  private loadEvent(): void {
    this.eventService.getEventById(this.eventId).subscribe({
      next: (event) => {
        this.event = event;
        this.isLoading = false;
      },
      error: () => {
        this.errorMessage = 'Could not load the event';
        this.isLoading = false;
      },
    });
  }

  private loadTemplates(): void {
    this.certificateService.getTemplates(this.eventId, this.event?.hackathon).subscribe({
      next: (templates) => (this.templates = templates),
      error: () => (this.errorMessage = 'Could not load the templates.'),
    });
  }

  private loadRuns(): void {
    this.certificateService.getRuns(this.eventId).subscribe({
      next: (runs) => {
        this.runs = runs;
        const running = runs.runs.find((r) => r.status === 'PENDING' || r.status === 'RUNNING');
        if(running) {
          this.activeRun = running;
          this.startPolling();
        }
      },
    });
  }

  private loadIssued(): void {
    this.certificateService.getIssuedForEvent(this.eventId).subscribe({
      next: (issued) => (this.issued = issued),
    });
  }

  selectTemplate(template: string): void{
    this.certificateService.getTemplate(templateId).subscribe({
      next: (template) => {
        this.selectedTemplateId = template.templateId;
        this.templateName = template.name;
        this.layout = template.layout;
        this.backgroundUrl = template.backgroundUrl;
        this.backgroundFile = null;
        this.assetUrlByKey = template.assetUrls || {};
        this.selectedElementIndex = null;
      },
    });
  }

  startNewTemplate(): void {
    this.selectedTemplateId = null;
    this.templateName = 'Untitled Certificate';
    this.layout = blankLayout();
    this.backgroundUrl = null;
    this.backgroundFile = null;
    this.assetUrlByKey = {};
    this.selectedElementIndex = null;
  }

  addTextElement(bound: boolean): void {
    const element: CertificateElement = {
      type: 'TEXT',
      field: bound ? 'participantName' : null,
      staticText: bound ? undefined : 'New label',
      x: this.canvasWidth/2,
      y: this.canvasHeight/2,
      font: 'helvetica',
      fontSize: 16,
      color: '#000000',
      align: 'center',
      visibleForTypes: null,
    };
    this.layout.elements.push(element);
    this.selectedElementIndex = this.layout.elements.length-1;
  }

  addQrElement(): void {
    const element: CertificateElement = {
      type: 'QR',
      x: this.canvasWidth-170,
      y: this.canvasHeight-130,
      fontSize: 18,
      visibleForTypes: null,
    };
    this.layout.element.push(element);
    this.selectedElementIndex = this.layout.elements.length-1;
  }

  onImageFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files && input.files[0];
    input.value = '';
    if(!file){
      return;
    }

    this.isUploadingAsset = true;
    this.errorMessage = '';
    this.ensureTemplateSaved().subscribe({
      next: (templateId) => {
        this.certificateService.uploadAsset(templateId, file).subscribe({
          next: (asset) => {
            this.assetUrlByKey[asset.storageKey] = asset.url;
            const element: CertificateElement = {
              type: 'IMAGE',
              imageStorageKey: asset.storageKey,
              label: file.name,
              width: 150,
              height: 80,
              x: this.canvasWidth/2-75,
              y: this.canvasHeight/2-40,
              visibleForTypes: null,
            };
            this.layout.elements.push(element);
            this.selectedElementIndex = this.layout.elements.length-1;
            this.isUploadingAsset = false;
        },
          error: () => {
            this.errorMessage = 'Could not upload the image';
            this.isUploadingAsset = false;
          },
        });
      },
      error: () => {
        this.errorMessage = 'Could not save the template before uploading the image';
        this.isUploadingAsset = false;
      },
    });
  }

  onReplaceImageSelected(event: Event): void{
    const input = event.target as HTMLInputElement;
    const file = input.files && input.files[0];
    input.value = '';
    const el = this.selectedElement;
    if(!file || !el || el.type !== 'IMAGE' || !this.selectedTemplateId){
      return;
    }

    this.isUploadingAsset = true;
    this.certificateService.uploadAsset(this.selectedTemplateId, file).subscribe({
      next: (asset) => {
        this.assetUrlByKey[asset.storageKey] =asset.url;
        el.imageStorageKey = asset.storageKey;
        el.label = file.name;
        this.isUploadingAsset = false;
      },
      error: () => {
        this.errorMessage = 'Could not upload the image';
        this.isUploadingAsset = false;
      },
    });
  }

  private ensureTemplateSaved(): Observable<string> {
    if(this.selectedTemplateId){
      const id = this.selectedTemplateId;
      return new Observable<string>((subscriber) => {
        subscriber.next(id);
        subscriber.complete();
      });
    }
    const req = {
      name: this.templateName,
      eventId: this.eventId,
      hackathonId: null,
      layout: this.layout,
    };
    return new Observable<string>((subscriber) => {
      this.certificateService.createTemplate(req).subscribe({
        next: (template: CertificateTemplateResponse) => {
          this.selectedTemplateId = template.templateId;
          this.loadTemplates();
          subscriber.next(template.templateId);
          subscriber.complete();
        },
        error: (err: unkown) => subscriber.error(err),
      });
    });
  }

  selectElement(index: number): void {
    this.selectedElementIndex = index;
  }

  deleteSelectedElement(): void {
    if (this.selectedElementIndex === null){
      return;
    }
    this.layout.elements.splice(this.selectedElementIndex, 1);
    this.selectedElementIndex = null;
  }

  isVisibleForType(element: CertificateElement, type: string): boolean {
    if(!element.visibleForTypes){
      return true;
    }
    return element.visibleForTypes
  }

  toggleVisibilityForType(type: string): void {
    const el = this.selectedElement;
    if(!el) return;
    const curr = el.visibleForTypes ? el.visibleForTypes.split(',').map((s) => s.trim()) : [...CERT_TYPES];
    const idx = curr.indexOf(type);
    if(idx >= 0){
      curr.splice(idx, 1);
    } else {
      curr.push(type);
    }
    el.visibleForTypes = current.length === CERT -TYPES.length ? null : current.join(',');
  }

  elementLabel(element: CertificateElement): string {
    if(element.type === 'QR') {
      return 'QR Code';
    }
    if(element.type === 'IMAGE') {
      return element.label || 'Image';
    }
    if(element.field) {
      return this.fieldOptions.find((f) => f.value === element.field)?.labal || element.field;
    }
    return element.staticText || 'Label';
  }

  previewText(element: CertificateElement): string {
    if(element.field === 'participantName') return 'Full name';
    if (element.field === 'teamName') return 'Team name';
    if (element.field === 'eventName') return this.event?.name || 'Sample name';
    if (element.field === 'rank') return '1st';
    if (element.field === 'certificateType') return 'WINNER';
    if (element.field === 'date') return 'Date issued';
    return element.staticText || '';
  }

  onElementMouseDown(event: MouseEvent, index: number): void {
    event.preventDefault();
    event.stopPropagation();
    this.selectedElementIndex = index;
    this.dragging = true;
    this.dragElementIndex = index;
    const el = this.layout.elements[index];
    const canvasRect = (event.currentTarget as HTMLElement)
      .closest('.designer-canvas')!
      .getBoundingClientRect();
    this.dragOffsetX = event.clientX-canvasRect.left-el.x;
    this.dragOffsetY = event.clientY-canvasRect.top-el.y;
  }

  onCanvasMouseMove(event: MouseEvent): void {
    if(!this.dragging){
      return;
    }
    const canvasRect = (event.currentTarget as HTMLElement).getBoundingClientRect();
    const el =this.layout.elements[this.dragElementIndex];
    el.x = Math.round(Math.max(0, Math.min(this.canvasWidth, event.clientX-canvasRect.left-this.dragOffsetX)));
    el.y = Math.round(Math.max(0, Math.min(this.canvasHeight, event.clientY-canvasRect.top-this.dragOffsetY)));
  }

  onCanvasMouseUp(): void {
    this.dragging = false;
  }

  onBackgroundSelected(event: Event): void {
    const input = event.target as HTMLElement;
    if(input.files && input.files.length > 0){
      this.backgroundFile = input.files[0];
    }
  }

  saveTemplate(): void {
    this.isSaving = true;
    this.errorMessage = '';
    this.successMessage = '';

    const req = {
      name: this.templateName,
      eventId: this.eventId,
      hackathonId: null,
      layout: this.layout,
    };

    const save$ = this.selectedTemplateId
      ? this.certificateService.uploadTemplate(this.selectedTemplateId, req)
      : this.certificateService.createTemplate(req);

    save$.subscribe({
      next: (template) => {
        this.selectedTemplateId = template.templateId;
        if(this.backgroundFile){
          this.certificateService.uploadBackground(template.templateId, this.backgroundFile).subscribe({
            next: (updated) => {
              this.backgroundUrl = updated.backgroundUrl;
              this.backgroundFile = null;
              this.finishSave();
            },
            error: () => {
              this.errorMessage = 'Template saved, but background image failed to upload';
              this.isSaving = false;
            },
          });
        } else {
          this.finishSave();
        }
        this.loadTemplates();
      },
      error: () => {
        this.errorMessage = 'Couldnt save the template';
        this.isSaving = false;
      },
    });
  }

  private finishSave(): void {
    this.isSaving = false;
    this.successMessage = 'Template saved';
    setTimeout(() => (this.successMessage = ''), 3000);
  }

  canGenerate(): boolean {
    return !!this.selectedTemplateId && !this.isGenerating;
  }

  startGeneration(): void {
    if(!this.selectedTemplateId){
      return;
    }
    this.isGenerating = true;
    this.errorMessage = '';

    this.certificateService
      .generate(this.eventId, {
        templateId: this.selectedTemplateId,
        scope: this.generationScope,
        topN: this.generationScope === 'TOP_N' ? this.topN : null,
      })
      .subscribe({
        next: (run) => {
          this.activeRun = run;
          this.startPolling();
        },
        error: () => {
          this.errorMessage = 'Could not start generation';
          this.isGenerating = false;
        },
      });
  }

  private startPolling(): void {
    this.isGenerating = true;
    if(this.pollHandle){
      clearInterval(this.pollHandle);
    }
    this.pollHandle = setInterval(() => {
      if(!this.activeRun){
        return;
      }
      this.certificateService.getRun(this.activeRun.runId).subscribe({
        next: (run) => {
          this.activeRun = run;
          if (run.status === 'COMPLETED' || run.status === 'FAILED'){
            this.isGenerating = false;
            if(this.pollHandle){
              clearInterval(this.pollHandle);
              this.pollHandle = null;
            }
            this.loadIssued();
            this.loadRuns();
          }
        },
      });
    }, 2000);
  }

  progressPercent(): number{
    if(!this.activeRun || this.activeRun.totalCount === 0){
      return 0;
    }
    return Math.round((this.activeRun.completedCount/this.activeRun.totalCount)*100);
  }

  goBack(): void {
    this.router.navigate(['/admin/events']);
  }
}
