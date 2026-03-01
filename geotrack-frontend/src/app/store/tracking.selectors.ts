import { createFeatureSelector, createSelector } from '@ngrx/store';
import { TrackingState, positionAdapter, alertAdapter } from './tracking.state';

export const selectTrackingState = createFeatureSelector<TrackingState>('tracking');

// Position selectors
const { selectAll: selectAllPositions, selectEntities: selectPositionEntities } =
  positionAdapter.getSelectors();

export const selectPositions = createSelector(
  selectTrackingState,
  (state) => selectAllPositions(state.positions)
);

export const selectPositionEntitiesMap = createSelector(
  selectTrackingState,
  (state) => selectPositionEntities(state.positions)
);

export const selectSelectedAssetId = createSelector(
  selectTrackingState,
  (state) => state.selectedAssetId
);

export const selectSelectedAssetPosition = createSelector(
  selectPositionEntitiesMap,
  selectSelectedAssetId,
  (entities, assetId) => assetId ? entities[assetId] ?? null : null
);

export const selectPositionCount = createSelector(
  selectPositions,
  (positions) => positions.length
);

export const selectLoading = createSelector(
  selectTrackingState,
  (state) => state.loading
);

export const selectError = createSelector(
  selectTrackingState,
  (state) => state.error
);

// Alert selectors
const { selectAll: selectAllAlerts } = alertAdapter.getSelectors();

export const selectAlerts = createSelector(
  selectTrackingState,
  (state) => selectAllAlerts(state.alerts)
);

export const selectUnacknowledgedAlerts = createSelector(
  selectAlerts,
  (alerts) => alerts.filter(a => !a.acknowledged)
);

export const selectAlertCount = createSelector(
  selectUnacknowledgedAlerts,
  (alerts) => alerts.length
);
