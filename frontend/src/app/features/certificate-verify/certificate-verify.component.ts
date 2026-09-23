import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { CertificateService, CertificateVerificationResponse } from '../../services/certificate.service';

@Component({
  selector: 'app-certificate-verify',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './certificate-verify.component.html',
  styleUrl: './certificate-verify.component.scss',
})
export class CertificateVerifyComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly certificateService = inject(CertificateService);
  isLoading = true;
  result: CertificateVerificationResponse | null = null;

  ngOnInit(): void {
    const code = this.route.snapshot.paramMap.get('code') || '';
    this.certificateService.verify(code).subscribe({
      next: (res) => {
        this.result = res;
        this.isLoading = false;
      },
      error: () => {
        this.result = { valid: false };
        this.isLoading = false;
      },
    });
  }
}
