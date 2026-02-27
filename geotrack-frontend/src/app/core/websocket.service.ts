import { Injectable, OnDestroy } from '@angular/core';
import { Observable, Subject, timer, EMPTY } from 'rxjs';
import { webSocket, WebSocketSubject } from 'rxjs/webSocket';
import { filter, map, retry, share, switchMap, catchError } from 'rxjs/operators';
import { AssetPosition, Alert, TrackingEvent } from './models';
import { environment } from './environment';

/**
 * WebSocket service for real-time position and alert streaming.
 *
 * Uses RxJS WebSocketSubject for automatic reconnection and
 * splits the single socket into typed observable streams that
 * components can subscribe to independently.
 */
@Injectable({ providedIn: 'root' })
export class WebSocketService implements OnDestroy {

  private socket$!: WebSocketSubject<TrackingEvent>;
  private destroy$ = new Subject<void>();

  /** Observable stream of real-time position updates */
  readonly positionUpdates$: Observable<AssetPosition>;

  /** Observable stream of alert events (geofence breaches, speed violations) */
  readonly alerts$: Observable<Alert>;

  /** Connection status */
  readonly connected$ = new Subject<boolean>();

  constructor() {
    this.connect();

    // Split single WebSocket stream into typed observables
    const shared$ = this.socket$.pipe(
      retry({ delay: 3000 }),  // Auto-reconnect on failure with 3s backoff
      share()                   // Share single subscription across all consumers
    );

    this.positionUpdates$ = shared$.pipe(
      filter(event => event.type === 'POSITION_UPDATED'),
      map(event => event.payload as AssetPosition)
    );

    this.alerts$ = shared$.pipe(
      filter(event =>
        event.type === 'GEOFENCE_BREACHED' ||
        event.type === 'GEOFENCE_EXITED' ||
        event.type === 'SPEED_EXCEEDED'
      ),
      map(event => event.payload as Alert)
    );
  }

  private connect(): void {
    this.socket$ = webSocket({
      url: environment.wsUrl,
      openObserver: {
        next: () => {
          console.log('[WS] Connected to tracking stream');
          this.connected$.next(true);
        }
      },
      closeObserver: {
        next: () => {
          console.log('[WS] Disconnected from tracking stream');
          this.connected$.next(false);
        }
      }
    });
  }

  /** Send a message to the server (e.g., subscription filters) */
  send(message: any): void {
    if (this.socket$) {
      this.socket$.next(message);
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.socket$.complete();
  }
}
