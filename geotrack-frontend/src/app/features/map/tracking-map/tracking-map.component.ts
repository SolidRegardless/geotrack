import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
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
  private trailSegments = new Map<string, L.Polyline[]>();
  private trailPoints = new Map<string, { lat: number; lng: number; time: number }[]>();
  private geofenceLayers = new Map<string, L.Polygon>();
  private destroy$ = new Subject<void>();
  private trailFadeInterval: ReturnType<typeof setInterval> | null = null;

  /** Max trail points per asset before oldest are pruned */
  private readonly MAX_TRAIL_POINTS = 200;
  /** Trail fully fades after this many ms */
  private readonly TRAIL_MAX_AGE_MS = 30 * 60 * 1000; // 30 minutes

  connectionStatus = false;
  assetCount = 0;

  // SVG paths for each asset type (icons point NORTH, rotated by heading)
  private readonly iconSvgPaths: Record<string, string> = {
    BUS: 'assets/markers/bus.svg',
    TRAIN: 'assets/markers/train.svg',
    PLANE: 'assets/markers/plane.svg',
    BOAT: 'assets/markers/boat.svg'
  };

  private readonly defaultIcon = L.icon({
    iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
    shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
    iconSize: [25, 41],
    iconAnchor: [12, 41],
    popupAnchor: [1, -34]
  });

  /** Create a DivIcon with an <img> rotated to the given heading */
  private createRotatedIcon(svgPath: string, heading: number, size: number = 32): L.DivIcon {
    return L.divIcon({
      html: `<img src="${svgPath}" style="width:${size}px;height:${size}px;transform:rotate(${heading}deg);transform-origin:center;" />`,
      iconSize: [size, size],
      iconAnchor: [size / 2, size / 2],
      popupAnchor: [0, -size / 2],
      className: 'rotated-marker'
    });
  }

  /** Resolve asset type key from assetId naming convention */
  private getAssetTypeKey(assetId: string): string | null {
    const id = assetId.toUpperCase();
    if (id.startsWith('SHIP-')) return 'BOAT';
    if (id.includes('BUS')) return 'BUS';
    if (id.includes('TRAIN')) return 'TRAIN';
    if (id.includes('PLANE')) return 'PLANE';
    if (id.includes('BOAT')) return 'BOAT';
    return 'PLANE'; // Default to plane icon for live aircraft data
  }

  /** Get the appropriate icon (rotated DivIcon or default) */
  private getIconForAsset(assetId: string, heading: number = 0): L.Icon | L.DivIcon {
    const key = this.getAssetTypeKey(assetId);
    if (key && this.iconSvgPaths[key]) {
      return this.createRotatedIcon(this.iconSvgPaths[key], heading);
    }
    return this.defaultIcon;
  }

  /** Get trail colour for asset type */
  private getTrailColor(assetId: string): string {
    const id = assetId.toUpperCase();
    if (id.includes('BUS')) return '#2196F3';
    if (id.includes('TRAIN')) return '#4CAF50';
    if (id.includes('PLANE')) return '#F44336';
    if (id.includes('BOAT')) return '#00BCD4';
    return '#3388ff';
  }

  constructor(
    private apiService: ApiService,
    private wsService: WebSocketService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.initMap();
    this.loadInitialPositions();
    this.subscribeToUpdates();
    this.subscribeToAlerts();
    this.startTrailFadeTimer();

    this.wsService.connected$
      .pipe(takeUntil(this.destroy$))
      .subscribe(status => {
        this.connectionStatus = status;
        this.cdr.markForCheck();
      });
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
        this.cdr.markForCheck();

        // Fit map to show all assets
        if (positions.length > 0) {
          const group = new L.FeatureGroup(Array.from(this.markers.values()));
          this.map.fitBounds(group.getBounds().pad(0.1));
        }

        // Load recent position history to build initial trail lines
        positions.forEach(pos => {
          const from = new Date(Date.now() - 3600000).toISOString(); // last hour
          this.apiService.getPositionHistory(pos.assetId, from).subscribe({
            next: (history) => {
              // Sort oldest first so the trail draws in order
              history.sort((a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime());
              history.forEach(hp => this.appendToRoute(hp));
            },
            error: () => {} // Silently ignore — trails will build from live data
          });
        });
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
        this.cdr.markForCheck();
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

      // Rotate the icon to match heading
      const iconEl = existing.getElement()?.querySelector('img') as HTMLImageElement;
      if (iconEl) {
        iconEl.style.transform = `rotate(${position.heading}deg)`;
      }

      // Update route trail
      this.appendToRoute(position);
    } else {
      // New marker with heading-rotated icon
      const marker = L.marker(latLng, {
        icon: this.getIconForAsset(position.assetId, position.heading),
        title: position.assetId
      });

      marker.bindPopup(this.createPopupHtml(position));
      marker.addTo(this.map);
      this.markers.set(position.assetId, marker);

      // Start the route trail from the first position
      this.appendToRoute(position);
    }
  }

  private appendToRoute(position: AssetPosition): void {
    const point = {
      lat: position.latitude,
      lng: position.longitude,
      time: new Date(position.timestamp).getTime() || Date.now()
    };

    // Append to the point history for this asset
    let points = this.trailPoints.get(position.assetId);
    if (!points) {
      points = [];
      this.trailPoints.set(position.assetId, points);
    }
    points.push(point);

    // Prune oldest points if over limit
    while (points.length > this.MAX_TRAIL_POINTS) {
      points.shift();
    }

    // Rebuild the faded trail segments for this asset
    this.rebuildTrail(position.assetId);
  }

  /**
   * Rebuild all trail segments for an asset with opacity fading.
   * Each segment between two consecutive points gets an opacity
   * proportional to its age — newest segments are fully opaque,
   * oldest segments are nearly transparent.
   */
  private rebuildTrail(assetId: string): void {
    // Remove existing segments from the map
    const existing = this.trailSegments.get(assetId) || [];
    existing.forEach(seg => seg.remove());

    const points = this.trailPoints.get(assetId);
    if (!points || points.length < 2) {
      this.trailSegments.set(assetId, []);
      return;
    }

    const color = this.getTrailColor(assetId);
    const now = Date.now();
    const segments: L.Polyline[] = [];

    for (let i = 0; i < points.length - 1; i++) {
      const segmentTime = points[i + 1].time;
      const age = now - segmentTime;

      // Opacity: 1.0 for brand new, fading to 0.05 at max age
      const ageFraction = Math.min(age / this.TRAIL_MAX_AGE_MS, 1);
      const opacity = Math.max(0.05, 1.0 - ageFraction * 0.95);

      // Slightly thinner for older segments
      const weight = Math.max(1, 3 - ageFraction * 2);

      const seg = L.polyline(
        [[points[i].lat, points[i].lng], [points[i + 1].lat, points[i + 1].lng]],
        { color, weight, opacity, interactive: false }
      ).addTo(this.map);

      segments.push(seg);
    }

    this.trailSegments.set(assetId, segments);
  }

  /** Periodically refresh trail opacities so they continue fading over time */
  private startTrailFadeTimer(): void {
    this.trailFadeInterval = setInterval(() => {
      const now = Date.now();
      for (const [assetId, points] of this.trailPoints) {
        // Prune points older than max age
        while (points.length > 0 && (now - points[0].time) > this.TRAIL_MAX_AGE_MS) {
          points.shift();
        }
        this.rebuildTrail(assetId);
      }
    }, 15000); // Refresh fading every 15 seconds
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
    if (this.trailFadeInterval) {
      clearInterval(this.trailFadeInterval);
    }
    if (this.map) {
      this.map.remove();
    }
  }
}
