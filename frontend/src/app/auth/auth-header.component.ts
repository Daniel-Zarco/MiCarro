import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  output,
  signal
} from '@angular/core';

import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-auth-header',
  templateUrl: './auth-header.component.html',
  styleUrl: './auth-header.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '(document:keydown.escape)': 'closeMenu()'
  }
})
export class AuthHeaderComponent {

  protected readonly authService = inject(AuthService);

  readonly login = output<void>();
  readonly plans = output<void>();

  protected readonly menuOpen = signal(false);

  protected readonly initial = computed(() => {
    const name = this.authService.user()?.name ?? '';
    return name.charAt(0).toUpperCase();
  });

  protected toggleMenu(): void {
    this.menuOpen.update(open => !open);
  }

  protected closeMenu(): void {
    this.menuOpen.set(false);
  }

  protected openLogin(): void {
    this.menuOpen.set(false);
    this.login.emit();
  }

  protected openPlans(): void {
    this.menuOpen.set(false);
    this.plans.emit();
  }

  protected logout(): void {
    this.authService.logout();
    this.menuOpen.set(false);
  }
}
