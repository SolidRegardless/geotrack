/**
 * TypeScript interfaces matching the Java DTOs.
 * Single source of truth for frontend types.
 */

export interface AssetPosition {
  id: string;
  assetId: string;
  latitude: number;
  longitude: number;
  altitude: number;
  speed: number;
  heading: number;
  timestamp: string;
  source: string;
}

export interface Asset {
  id: string;
  name: string;
  type: AssetType;
  status: AssetStatus;
  createdAt: string;
  updatedAt: string;
}

export interface Geofence {
  id: string;
  name: string;
  description: string;
  fenceType: FenceType;
  coordinates: [number, number][];
  active: boolean;
}

export interface Alert {
  id: string;
  assetId: string;
  geofenceId: string;
  alertType: string;
  severity: Severity;
  message: string;
  acknowledged: boolean;
  createdAt: string;
}

export interface TrackingEvent {
  type: 'POSITION_UPDATED' | 'GEOFENCE_BREACHED' | 'GEOFENCE_EXITED' | 'SPEED_EXCEEDED';
  payload: AssetPosition | Alert;
}

export type AssetType = 'VEHICLE' | 'DRONE' | 'VESSEL' | 'PERSONNEL' | 'AIRCRAFT' | 'SENSOR';
export type AssetStatus = 'ACTIVE' | 'INACTIVE' | 'MAINTENANCE' | 'DECOMMISSIONED';
export type FenceType = 'INCLUSION' | 'EXCLUSION';
export type Severity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
