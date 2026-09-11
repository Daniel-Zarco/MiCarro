import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';

import { FavoriteService } from '../services/favorite.service';
import { FavoritesMigrationComponent } from './favorites-migration.component';

describe('FavoritesMigrationComponent', () => {

  function setup(pending: boolean) {
    const migrateLocalFavorites = vi.fn();
    const dismissMigration = vi.fn();

    TestBed.configureTestingModule({
      imports: [FavoritesMigrationComponent],
      providers: [
        {
          provide: FavoriteService,
          useValue: {
            hasPendingMigration: () => pending,
            pendingMigrationCount: () => (pending ? 2 : 0),
            migrateLocalFavorites,
            dismissMigration,
          },
        },
      ],
    });

    const fixture = TestBed.createComponent(FavoritesMigrationComponent);
    fixture.detectChanges();

    return { fixture, migrateLocalFavorites, dismissMigration };
  }

  it('no muestra el banner si no hay migración pendiente', () => {
    const { fixture } = setup(false);
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('.migration-banner')).toBeNull();
  });

  it('muestra el banner con el número de favoritos', () => {
    const { fixture } = setup(true);
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('.migration-banner')).not.toBeNull();
    expect(element.querySelector('.migration-text')?.textContent).toContain('2');
  });

  it('acepta la migración', () => {
    const { fixture, migrateLocalFavorites } = setup(true);
    const element = fixture.nativeElement as HTMLElement;

    element.querySelector<HTMLButtonElement>('.migration-accept')!.click();

    expect(migrateLocalFavorites).toHaveBeenCalled();
  });

  it('rechaza la migración', () => {
    const { fixture, dismissMigration } = setup(true);
    const element = fixture.nativeElement as HTMLElement;

    element.querySelector<HTMLButtonElement>('.migration-dismiss')!.click();

    expect(dismissMigration).toHaveBeenCalled();
  });
});
