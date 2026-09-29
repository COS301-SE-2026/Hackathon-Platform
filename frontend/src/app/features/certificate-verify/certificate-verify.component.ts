import { Component, OnInit, inject, signal } from '@angular/core';
import { DataPipe } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { CertificateService, CertificateVerificationResponse } from '../../services/certificate.service';

@Component({
  selector: 'app-certificate-verify',
  standalone: true,
  imports: [DataPipe],
  templateUrl: './certificate-verify.component.html',
  styleUrl: './certificate-verify.component.scss',
})
export class CertificateVerifyComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly certificateService = inject(CertificateService);
  readonly isLoading = signal(true);
  readonly result = signal<CertificateVerificationResponse | null>(null);

  ngOnInit(): void {
    const code = this.route.snapshot.paramMap.get('code') || '';
    this.certificateService.verify(code).subscribe({
      next: (res) => {
        this.result.set(res);
        this.isLoading.set(false);
      },
      error: () => {
        this.result.set({ valid: false });
        this.isLoading.set(false);
      },
    });
  }
}
