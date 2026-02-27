import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Asset, AssetPosition, Alert, Geofence, AssetType, AssetStatus } from './models';
import { environment } from './environment';

/**
 * HTTP client service for the GeoTrack REST API.
 * Wraps all API calls with proper typing.
 */
@Injectable({ providedIn: 'root' })
export class ApiService {

  private readonly baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  // ============================
  // Assets
  // ============================

  getAssets(type?: AssetType, status?: AssetStatus, page = 0, size = 20): Observable<Asset[]> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    if (type) params = params.set('type', type);
    if (status) params = params.set('status', status);
    return this.http.get<Asset[]>(`${this.baseUrl}/assets`, { params });
  }

  getAsset(id: string): Observable<Asset> {
    return this.http.get<Asset>(`${this.baseUrl}/assets/${id}`);
  }

  createAsset(asset: { name: string; type: AssetType }): Observable<Asset> {
    return this.http.post<Asset>(`${this.baseUrl}/assets`, asset);
  }

  deleteAsset(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/assets/${id}`);
  }

  // ============================
  // Positions
  // ============================

  getLatestPositions(): Observable<AssetPosition[]> {
    return this.http.get<AssetPosition[]>(`${this.baseUrl}/positions/latest`);
  }

  getPositionHistory(assetId: string, from?: string, to?: string): Observable<AssetPosition[]> {
    let params = new HttpParams();
    if (from) params = params.set('from', from);
    if (to) params = params.set('to', to);
    return this.http.get<AssetPosition[]>(`${this.baseUrl}/assets/${assetId}/positions`, { params });
  }

  submitPosition(position: {
    assetId: string;
    latitude: number;
    longitude: number;
    speed: number;
    heading: number;
    timestamp: string;
  }): Observable<AssetPosition> {
    return this.http.post<AssetPosition>(`${this.baseUrl}/positions`, position);
  }

  // ============================
  // Geofences
  // ============================

  getGeofences(): Observable<Geofence[]> {
    return this.http.get<Geofence[]>(`${this.baseUrl}/geofences`);
  }

  createGeofence(geofence: Partial<Geofence>): Observable<Geofence> {
    return this.http.post<Geofence>(`${this.baseUrl}/geofences`, geofence);
  }

  // ============================
  // Alerts
  // ============================

  getAlerts(acknowledged?: boolean): Observable<Alert[]> {
    let params = new HttpParams();
    if (acknowledged !== undefined) params = params.set('acknowledged', acknowledged.toString());
    return this.http.get<Alert[]>(`${this.baseUrl}/alerts`, { params });
  }

  acknowledgeAlert(id: string): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/alerts/${id}/acknowledge`, {});
  }
}
