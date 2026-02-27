import { Component, OnInit, OnDestroy, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subject, takeUntil } from 'rxjs';
import * as L from 'leaflet';
import { ApiService } from '../../../core/api.service';
import { WebSocketService } from '../../../core/websocket.service';
import { AssetPosition } from '../../../core/models';

/**
 * Real-time tracking map component.
 *
 * Displays asset positions on a Leaflet map with live WebSocket updates.
 * Markers animate smoothly to new positions as telemetry arrives.
 */
@Component({
  selector: 'app-tracking-map',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './tracking-map.component.html',
  styleUrls: ['./tracking-map.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TrackingMapComponent implements OnInit, OnDestroy {

  private map!: L.Map;
  private markers = new Map<string, L.Marker>();
  private routeLines = new Map<string, L.Polyline>();
  private geofenceLayers = new Map<string, L.Polygon>();
  private destroy$ = new Subject<void>();

  connectionStatus = false;
  assetCount = 0;

  // Custom icons for different asset types
  private icons: Record<string, L.Icon> = {
    VEHICLE: L.icon({
      iconUrl: 'assets/markers/vehicle.png',
      iconSize: [32, 32],
      iconAnchor: [16, 32],
      popupAnchor: [0, -32]
    }),
    DRONE: L.icon({
      iconUrl: 'assets/markers/drone.png',
      iconSize: [32, 32],
      iconAnchor: [16, 16],
      popupAnchor: [0, -16]
    }),
    VESSEL: L.icon({
      iconUrl: 'assets/markers/vessel.png',
      iconSize: [32, 32],
      iconAnchor: [16, 32],
      popupAnchor: [0, -32]
    }),
    DEFAULT: L.icon({
      iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
      shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
      iconSize: [25, 41],
      iconAnchor: [12, 41],
      popupAnchor: [1, -34]
    })
  };

  constructor(
    private apiService: ApiService,
    private wsService: WebSocketService
  ) {}

  ngOnInit(): void {
    this.initMap();
    this.loadInitialPositions();
    this.subscribeToUpdates();
    this.subscribeToAlerts();

    this.wsService.connected$
      .pipe(takeUntil(this.destroy$))
      .subscribe(status => this.connectionStatus = status);
  }

  private initMap(): void {
    // Centre on Newcastle
    this.map = L.map('tracking-map').setView([54.9783, -1.6178], 13);

    // Base tile layers
    const osm = L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
      maxZoom: 19
    });

    const dark = L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
      attribution: '&copy; <a href="https://carto.com/">CARTO</a>',
      maxZoom: 19
    });

    const satellite = L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}', {
      attribution: '&copy; Esri',
      maxZoom: 18
    });

    // Add default layer
    osm.addTo(this.map);

    // Layer control
    L.control.layers({
      'OpenStreetMap': osm,
      'Dark Mode': dark,
      'Satellite': satellite
    }).addTo(this.map);

    // Scale bar
    L.control.scale({ imperial: false }).addTo(this.map);
  }

  private loadInitialPositions(): void {
    this.apiService.getLatestPositions().subscribe({
      next: (positions) => {
        positions.forEach(pos => this.updateMarker(pos));
        this.assetCount = positions.length;

        // Fit map to show all assets
        if (positions.length > 0) {
          const group = new L.FeatureGroup(Array.from(this.markers.values()));
          this.map.fitBounds(group.getBounds().pad(0.1));
        }
      },
      error: (err) => console.error('Failed to load initial positions:', err)
    });
  }

  private subscribeToUpdates(): void {
    this.wsService.positionUpdates$
      .pipe(takeUntil(this.destroy$))
      .subscribe(position => {
        this.updateMarker(position);
        this.assetCount = this.markers.size;
      });
  }

  private subscribeToAlerts(): void {
    this.wsService.alerts$
      .pipe(takeUntil(this.destroy$))
      .subscribe(alert => {
        console.warn('Alert received:', alert);
        // Flash the marker red, show notification toast
      });
  }

  private updateMarker(position: AssetPosition): void {
    const latLng: L.LatLngExpression = [position.latitude, position.longitude];
    const existing = this.markers.get(position.assetId);

    if (existing) {
      // Smooth update — move existing marker
      existing.setLatLng(latLng);
      existing.setPopupContent(this.createPopupHtml(position));

      // Update route trail
      this.appendToRoute(position);
    } else {
      // New marker
      const marker = L.marker(latLng, {
        icon: this.icons['DEFAULT'],
        title: position.assetId
      });

      marker.bindPopup(this.createPopupHtml(position));
      marker.addTo(this.map);
      this.markers.set(position.assetId, marker);
    }
  }

  private appendToRoute(position: AssetPosition): void {
    const latLng: L.LatLngTuple = [position.latitude, position.longitude];
    const existing = this.routeLines.get(position.assetId);

    if (existing) {
      existing.addLatLng(latLng);
    } else {
      const line = L.polyline([latLng], {
        color: '#3388ff',
        weight: 2,
        opacity: 0.6
      }).addTo(this.map);
      this.routeLines.set(position.assetId, line);
    }
  }

  private createPopupHtml(position: AssetPosition): string {
    return `
      <div class="asset-popup">
        <h4>${position.assetId}</h4>
        <table>
          <tr><td><strong>Lat:</strong></td><td>${position.latitude.toFixed(6)}</td></tr>
          <tr><td><strong>Lon:</strong></td><td>${position.longitude.toFixed(6)}</td></tr>
          <tr><td><strong>Speed:</strong></td><td>${position.speed.toFixed(1)} km/h</td></tr>
          <tr><td><strong>Heading:</strong></td><td>${position.heading.toFixed(0)}&deg;</td></tr>
          <tr><td><strong>Alt:</strong></td><td>${position.altitude.toFixed(0)} m</td></tr>
          <tr><td><strong>Time:</strong></td><td>${new Date(position.timestamp).toLocaleTimeString()}</td></tr>
        </table>
      </div>
    `;
  }

  /** Zoom to a specific asset */
  zoomToAsset(assetId: string): void {
    const marker = this.markers.get(assetId);
    if (marker) {
      this.map.setView(marker.getLatLng(), 16);
      marker.openPopup();
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    if (this.map) {
      this.map.remove();
    }
  }
}
