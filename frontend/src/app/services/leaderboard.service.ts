import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface LeaderboardEntry {
    rank: number;
    teamId: string;
    teamName: string;
    bestScore: number;
    lastScoredAt: string | null;
}