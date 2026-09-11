import {
  ChangeDetectionStrategy,
  Component,
  inject
} from '@angular/core';

import { FavoriteService } from '../services/favorite.service';

@Component({
  selector: 'app-favorites-migration',
  templateUrl: './favorites-migration.component.html',
  styleUrl: './favorites-migration.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FavoritesMigrationComponent {

  protected readonly favoriteService = inject(FavoriteService);

  protected accept(): void {
    this.favoriteService.migrateLocalFavorites();
  }

  protected dismiss(): void {
    this.favoriteService.dismissMigration();
  }
}
