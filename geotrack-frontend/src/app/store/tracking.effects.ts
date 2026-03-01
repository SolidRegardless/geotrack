import { Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { of } from 'rxjs';
import { map, exhaustMap, catchError } from 'rxjs/operators';
import { ApiService } from '../core/api.service';
import { TrackingActions } from './tracking.actions';

@Injectable()
export class TrackingEffects {

  loadLatestPositions$ = createEffect(() =>
    this.actions$.pipe(
      ofType(TrackingActions.loadLatestPositions),
      exhaustMap(() =>
        this.apiService.getLatestPositions().pipe(
          map(positions => TrackingActions.loadLatestPositionsSuccess({ positions })),
          catchError(error => of(TrackingActions.loadLatestPositionsFailure({ error: error.message })))
        )
      )
    )
  );

  loadAlerts$ = createEffect(() =>
    this.actions$.pipe(
      ofType(TrackingActions.loadAlerts),
      exhaustMap(() =>
        this.apiService.getAlerts().pipe(
          map(alerts => TrackingActions.loadAlertsSuccess({ alerts })),
          catchError(error => of(TrackingActions.loadAlertsFailure({ error: error.message })))
        )
      )
    )
  );

  constructor(
    private actions$: Actions,
    private apiService: ApiService
  ) {}
}
