import { createActionGroup, emptyProps, props } from '@ngrx/store';
import { AssetPosition, Alert } from '../core/models';

export const TrackingActions = createActionGroup({
  source: 'Tracking',
  events: {
    // Positions
    'Load Latest Positions': emptyProps(),
    'Load Latest Positions Success': props<{ positions: AssetPosition[] }>(),
    'Load Latest Positions Failure': props<{ error: string }>(),
    'Position Updated': props<{ position: AssetPosition }>(),

    // Selected asset
    'Select Asset': props<{ assetId: string }>(),
    'Clear Selected Asset': emptyProps(),

    // Alerts
    'Load Alerts': emptyProps(),
    'Load Alerts Success': props<{ alerts: Alert[] }>(),
    'Load Alerts Failure': props<{ error: string }>(),
    'Alert Received': props<{ alert: Alert }>(),
    'Acknowledge Alert': props<{ alertId: string }>(),
  }
});
