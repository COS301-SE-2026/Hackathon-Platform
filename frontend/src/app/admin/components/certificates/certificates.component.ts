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
