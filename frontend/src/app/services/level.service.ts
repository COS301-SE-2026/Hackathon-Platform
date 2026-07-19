import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface LevelRequest {
  name: string;
  levelNumber: number;
  description?: string;
}

export interface LevelResponse {
  id: number;
  hackathonId: string;
  name: string;
  levelNumber: number;
  description?: string;
}