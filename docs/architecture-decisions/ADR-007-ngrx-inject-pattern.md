# ADR-007: NgRx inject() Over Constructor Injection for Effects

**Status:** Accepted  
**Date:** 2026-03-01  
**Decision Makers:** GeoTrack Frontend Team

## Context

In Angular 19 with NgRx, Effects classes declare reactive pipelines using `createEffect()` assigned as class field initializers. These initializers depend on injected services (`Actions`, API services, etc.).

JavaScript class field initializers execute **before** the constructor body runs. When dependencies are provided via constructor parameters, they are `undefined` at the time field initializers evaluate — causing runtime errors such as `TypeError: Cannot read properties of undefined`.

This has been a recurring source of bugs in GeoTrack's asset tracking Effects, particularly as the team scales and new developers follow older Angular patterns.

## Decision

Use Angular's `inject()` function for **all** dependencies in NgRx Effects classes instead of constructor injection.

`inject()` resolves dependencies from the injection context during field initialization, because Angular establishes the injection context before any class fields or constructors run. This guarantees dependencies are available when `createEffect()` field initializers execute.

### ❌ Wrong — Constructor Injection

```typescript
@Injectable()
export class AssetEffects {
  // BUG: this.actions$ is undefined when this field initializer runs
  loadAssets$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AssetActions.loadAssets),
      switchMap(() =>
        this.assetService.getAll().pipe(
          map(assets => AssetActions.loadAssetsSuccess({ assets })),
          catchError(error => of(AssetActions.loadAssetsFailure({ error })))
        )
      )
    )
  );

  constructor(
    private actions$: Actions,
    private assetService: AssetService
  ) {}
}
```

### ✅ Right — inject() Function

```typescript
@Injectable()
export class AssetEffects {
  private actions$ = inject(Actions);
  private assetService = inject(AssetService);

  loadAssets$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AssetActions.loadAssets),
      switchMap(() =>
        this.assetService.getAll().pipe(
          map(assets => AssetActions.loadAssetsSuccess({ assets })),
          catchError(error => of(AssetActions.loadAssetsFailure({ error })))
        )
      )
    )
  );
}
```

## Consequences

### Positive

- **Eliminates runtime errors** from undefined dependencies during field initialization
- **Simpler classes** — no constructor boilerplate needed
- **Consistent with Angular 19 idioms** — `inject()` is the recommended DI approach for modern Angular
- **Easier to add dependencies** — add a field, no constructor signature to maintain
- **Tree-shakable** — `inject()` works well with Angular's tree-shaking optimisation

### Negative

- **Breaks from legacy patterns** — developers coming from older Angular codebases may default to constructor injection out of habit
- **Requires team alignment** — existing Effects classes must be migrated; enforce via lint rules

### Neutral

- Applies to all NgRx Effects classes; other injectable classes may use either pattern without this specific issue, though `inject()` is preferred project-wide for consistency

## References

- [Angular Frontend Runbook](../runbooks/angular-frontend-runbook.md)
- [Angular `inject()` documentation](https://angular.dev/api/core/inject)
- [NgRx Effects documentation](https://ngrx.io/guide/effects)
