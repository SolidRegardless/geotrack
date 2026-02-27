import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../../core/api.service';
import { Alert } from '../../../core/models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {

  kpis = [
    { label: 'Total Assets', value: '—', icon: '📡' },
    { label: 'Active Alerts', value: '—', icon: '🚨' },
    { label: 'Positions Today', value: '—', icon: '📍' },
    { label: 'Geofences Active', value: '—', icon: '🔲' }
  ];

  recentAlerts: Alert[] = [];

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.api.getAssets().subscribe({
      next: (assets) => this.kpis[0].value = assets.length.toString(),
      error: () => this.kpis[0].value = '?'
    });

    this.api.getAlerts(false).subscribe({
      next: (alerts) => {
        this.kpis[1].value = alerts.length.toString();
        this.recentAlerts = alerts
          .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
          .slice(0, 10);
      },
      error: () => this.kpis[1].value = '?'
    });

    this.api.getGeofences().subscribe({
      next: (gf) => this.kpis[3].value = gf.filter(g => g.active).length.toString(),
      error: () => this.kpis[3].value = '?'
    });

    // Positions today — no dedicated endpoint, use placeholder
    this.kpis[2].value = '—';
  }

  severityColor(severity: string): string {
    const colors: Record<string, string> = {
      LOW: '#44ff44', MEDIUM: '#ffbb33', HIGH: '#ff8800', CRITICAL: '#ff4444'
    };
    return colors[severity] || '#aaa';
  }
}
