import { Routes } from '@angular/router';
import { TrackingMapComponent } from './features/map/tracking-map/tracking-map.component';
import { AssetListComponent } from './features/assets/asset-list/asset-list.component';
import { AlertListComponent } from './features/alerts/alert-list/alert-list.component';
import { DashboardComponent } from './features/dashboard/dashboard/dashboard.component';

export const routes: Routes = [
  { path: '', redirectTo: '/map', pathMatch: 'full' },
  { path: 'map', component: TrackingMapComponent },
  { path: 'assets', component: AssetListComponent },
  { path: 'alerts', component: AlertListComponent },
  { path: 'dashboard', component: DashboardComponent },
];
