import { EntityState, EntityAdapter, createEntityAdapter } from '@ngrx/entity';
import { AssetPosition, Alert } from '../core/models';

export interface TrackingState {
  positions: EntityState<AssetPosition>;
  alerts: EntityState<Alert>;
  selectedAssetId: string | null;
  loading: boolean;
  error: string | null;
}

export const positionAdapter: EntityAdapter<AssetPosition> = createEntityAdapter<AssetPosition>({
  selectId: (position) => position.assetId,
  sortComparer: (a, b) => a.assetId.localeCompare(b.assetId)
});

export const alertAdapter: EntityAdapter<Alert> = createEntityAdapter<Alert>({
  selectId: (alert) => alert.id,
  sortComparer: (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
});

export const initialTrackingState: TrackingState = {
  positions: positionAdapter.getInitialState(),
  alerts: alertAdapter.getInitialState(),
  selectedAssetId: null,
  loading: false,
  error: null
};
