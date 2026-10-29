
import { Component, signal, OnInit, OnDestroy, inject } from '@angular/core';
import { ViewportScroller } from '@angular/common';
import { RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { ToastComponent } from './shared/components/toast/toast.component';
import { filter, Subscription } from 'rxjs';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, ToastComponent],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit, OnDestroy {
  protected readonly title = signal('frontend');

  private readonly router = inject(Router);
  private readonly viewportScroller = inject(ViewportScroller);

  private scrollSubscription?: Subscription;
  private previousPath = this.router.url.split(/[?#]/)[0];

  ngOnInit(): void {
    this.scrollSubscription = this.router.events
      .pipe(
        filter(
          (event): event is NavigationEnd =>
            event instanceof NavigationEnd
        )
      )
      .subscribe(event => {
        const currentPath = event.urlAfterRedirects.split(/[?#]/)[0];

        if (currentPath !== this.previousPath) {
          this.viewportScroller.scrollToPosition([0, 0]);
        }

        this.previousPath = currentPath;
      });
  }

  ngOnDestroy(): void {
    this.scrollSubscription?.unsubscribe();
  }
}
