import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { of } from 'rxjs';
import { SubmitComponent } from './submit.component';
import { StorageService } from '../../services/storage.service';

describe('SubmitComponent', () => {
  let component: SubmitComponent;
  let fixture: ComponentFixture<SubmitComponent>;
  let storageMock: jasmine.SpyObj<StorageService>;

  beforeEach(async () => {
    localStorage.setItem('currentEventId', 'event-123');
    localStorage.setItem('currentTeamId', 'team-123');

    storageMock = jasmine.createSpyObj<StorageService>('StorageService', [
      'uploadSubmissionOutput',
      'uploadSubmissionSource',
      'getLevelFileUrl'
    ]);

    storageMock.uploadSubmissionOutput.and.returnValue(of({ ok: true } as any));
    storageMock.uploadSubmissionSource.and.returnValue(of({ ok: true } as any));
    storageMock.getLevelFileUrl.and.returnValue(of({ url: 'https://example.com/file' } as any));

    await TestBed.configureTestingModule({
      imports: [RouterTestingModule, SubmitComponent],
      providers: [
        { provide: StorageService, useValue: storageMock },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { queryParams: {}, params: {} },
            queryParams: of({}),
            params: of({})
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(SubmitComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have three resources', () => {
    expect(component.resources.length).toBe(3);
  });

  it('should set outputFileName when a file is selected', () => {
    const fakeFile = new File(['content'], 'output.txt', { type: 'text/plain' });
    const event = { target: { files: [fakeFile] } } as unknown as Event;
    component.onOutputSelected(event);

    expect(component.outputFileName).toBe('output.txt');
    expect(component.outputFile).toBe(fakeFile);
  });

  it('should set zipFileName when a file is selected', () => {
    const fakeFile = new File(['content'], 'code.zip', { type: 'application/zip' });
    const event = { target: { files: [fakeFile] } } as unknown as Event;
    component.onZipSelected(event);

    expect(component.zipFileName).toBe('code.zip');
    expect(component.zipFile).toBe(fakeFile);
  });

  it('should show alert when onSubmit is called without files', () => {

    component.outputFile = null;
    component.zipFile = null;
    component.onSubmit();
  });

  it('should upload submission when both files are selected', async () => {
    spyOn(crypto, 'randomUUID').and.returnValue('11111111-1111-4111-8111-111111111111');

    const outputFile = new File(['output'], 'output.txt', { type: 'text/plain' });
    const zipFile = new File(['code'], 'code.zip', { type: 'application/zip' });

    component.outputFile = outputFile;
    component.zipFile = zipFile;
    component.outputFileName = 'output.txt';
    component.zipFileName = 'code.zip';

    component.onSubmit();
    await fixture.whenStable();

    expect(storageMock.uploadSubmissionOutput).toHaveBeenCalledWith(
      'event-123',
      'team-123',
      '11111111-1111-4111-8111-111111111111',
      outputFile
    );
    expect(storageMock.uploadSubmissionSource).toHaveBeenCalledWith(
      'event-123',
      'team-123',
      '11111111-1111-4111-8111-111111111111',
      zipFile
    );
    expect(storageMock.uploadSubmissionOutput).toHaveBeenCalled();
    expect(storageMock.uploadSubmissionSource).toHaveBeenCalled();
  });

  it('should set outputFileName on drop', () => {
  const fakeFile = new File(['content'], 'dropped.csv', { type: 'text/csv' });
  const dragEvent = {
    preventDefault: () => {/* */},
    dataTransfer: { files: [fakeFile] }
  } as unknown as DragEvent;
  component.onDropOutput(dragEvent);
  expect(component.outputFileName).toBe('dropped.csv');
});

it('should not set outputFileName when drop has no file', () => {
  const dragEvent = {
    preventDefault: () => {/* */},
    dataTransfer: { files: [] }
  } as unknown as DragEvent;
  component.onDropOutput(dragEvent);
  expect(component.outputFileName).toBe('');
});

it('should set zipFileName on drop', () => {
  const fakeFile = new File(['content'], 'archive.zip', { type: 'application/zip' });
  const dragEvent = {
    preventDefault: () => {/* */},
    dataTransfer: { files: [fakeFile] }
  } as unknown as DragEvent;
  component.onDropZip(dragEvent);
  expect(component.zipFileName).toBe('archive.zip');
});

it('should not set zipFileName when drop has no file', () => {
  const dragEvent = {
    preventDefault: () => {/* */},
    dataTransfer: { files: [] }
  } as unknown as DragEvent;
  component.onDropZip(dragEvent);
  expect(component.zipFileName).toBe('');
});
});