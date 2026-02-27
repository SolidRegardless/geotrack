import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterModule],
  template: `
    <nav class="navbar">
      <div class="navbar-brand">
        <span class="logo">🌍</span>
        <span class="title">GeoTrack</span>
      </div>
      <div class="navbar-links">
        <a routerLink="/map" routerLinkActive="active">Map</a>
        <a routerLink="/assets" routerLinkActive="active">Assets</a>
        <a routerLink="/alerts" routerLinkActive="active">Alerts</a>
        <a routerLink="/dashboard" routerLinkActive="active">Dashboard</a>
      </div>
    </nav>
  `,
  styles: [`
    .navbar {
      display: flex;
      align-items: center;
      justify-content: space-between;
      background: #1a1a2e;
      color: white;
      padding: 0 24px;
      height: 64px;

      .navbar-brand {
        display: flex;
        align-items: center;
        gap: 12px;
        .logo { font-size: 28px; }
        .title { font-size: 20px; font-weight: 700; }
      }

      .navbar-links {
        display: flex;
        gap: 8px;
        a {
          color: #aaa;
          text-decoration: none;
          padding: 8px 16px;
          border-radius: 6px;
          transition: all 0.2s;
          &:hover { color: white; background: rgba(255,255,255,0.1); }
          &.active { color: white; background: #4695EB; }
        }
      }
    }
  `]
})
export class NavbarComponent {}
