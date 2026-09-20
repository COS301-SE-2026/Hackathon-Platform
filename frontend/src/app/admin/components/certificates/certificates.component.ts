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
  { value: 'participantName', label: 'Partcicipant Name' },
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
}
