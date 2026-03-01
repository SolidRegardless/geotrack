import { createReducer, on } from '@ngrx/store';
import { TrackingActions } from './tracking.actions';
import { initialTrackingState, positionAdapter, alertAdapter, TrackingState } from './tracking.state';

export const trackingReducer = createReducer(
  initialTrackingState,

  // Positions
  on(TrackingActions.loadLatestPositions, (state): TrackingState => ({
    ...state,
    loading: true,
    error: null
  })),

  on(TrackingActions.loadLatestPositionsSuccess, (state, { positions }): TrackingState => ({
    ...state,
    positions: positionAdapter.upsertMany(positions, state.positions),
    loading: false
  })),

  on(TrackingActions.loadLatestPositionsFailure, (state, { error }): TrackingState => ({
    ...state,
    loading: false,
    error
  })),

  on(TrackingActions.positionUpdated, (state, { position }): TrackingState => ({
    ...state,
    positions: positionAdapter.upsertOne(position, state.positions)
  })),

  // Selected asset
  on(TrackingActions.selectAsset, (state, { assetId }): TrackingState => ({
    ...state,
    selectedAssetId: assetId
  })),

  on(TrackingActions.clearSelectedAsset, (state): TrackingState => ({
    ...state,
    selectedAssetId: null
  })),

  // Alerts
  on(TrackingActions.loadAlertsSuccess, (state, { alerts }): TrackingState => ({
    ...state,
    alerts: alertAdapter.setAll(alerts, state.alerts)
  })),

  on(TrackingActions.alertReceived, (state, { alert }): TrackingState => ({
    ...state,
    alerts: alertAdapter.addOne(alert, state.alerts)
  }))
);
