import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subject, takeUntil } from 'rxjs';
import { ApiService } from '../../../core/api.service';
import { WebSocketService } from '../../../core/websocket.service';
import { Alert } from '../../../core/models';

@Component({
  selector: 'app-alert-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './alert-list.component.html',
  styleUrl: './alert-list.component.scss'
})
export class AlertListComponent implements OnInit, OnDestroy {

  alerts: Alert[] = [];
  showUnacknowledgedOnly = false;
  private destroy$ = new Subject<void>();

  constructor(
    private api: ApiService,
    private ws: WebSocketService
  ) {}

  ngOnInit(): void {
    this.loadAlerts();

    this.ws.alerts$
      .pipe(takeUntil(this.destroy$))
      .subscribe(alert => {
        this.alerts = [alert, ...this.alerts];
      });
  }

  loadAlerts(): void {
    const ack = this.showUnacknowledgedOnly ? false : undefined;
    this.api.getAlerts(ack).subscribe({
      next: (alerts) => this.alerts = alerts.sort((a, b) =>
        new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
      ),
      error: (err) => console.error('Failed to load alerts:', err)
    });
  }

  get filteredAlerts(): Alert[] {
    if (!this.showUnacknowledgedOnly) return this.alerts;
    return this.alerts.filter(a => !a.acknowledged);
  }

  toggleFilter(): void {
    this.showUnacknowledgedOnly = !this.showUnacknowledgedOnly;
  }

  acknowledge(alert: Alert, event: Event): void {
    event.stopPropagation();
    this.api.acknowledgeAlert(alert.id).subscribe({
      next: () => { alert.acknowledged = true; },
      error: (err) => console.error('Failed to acknowledge:', err)
    });
  }

  severityColor(severity: string): string {
    const colors: Record<string, string> = {
      LOW: '#44ff44', MEDIUM: '#ffbb33', HIGH: '#ff8800', CRITICAL: '#ff4444'
    };
    return colors[severity] || '#aaa';
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
