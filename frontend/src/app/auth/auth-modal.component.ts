import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  output,
  signal
} from '@angular/core';
import {
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { AuthService } from '../services/auth.service';

export type AuthMode = 'login' | 'register';

@Component({
  selector: 'app-auth-modal',
  imports: [ReactiveFormsModule],
  templateUrl: './auth-modal.component.html',
  styleUrl: './auth-modal.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '(document:keydown.escape)': 'close()'
  }
})
export class AuthModalComponent {

  private readonly authService = inject(AuthService);

  readonly closed = output<void>();

  readonly mode = signal<AuthMode>('login');
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly submitted = signal(false);

  readonly form = new FormGroup({
    name: new FormControl('', { nonNullable: true }),
    email: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.email]
    }),
    password: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(8)]
    })
  });

  readonly title = computed(() =>
    this.mode() === 'login' ? 'Iniciar sesión' : 'Crear cuenta'
  );

  submit(): void {

    this.submitted.set(true);

    if (this.form.invalid || this.loading()) {
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    const { name, email, password } = this.form.getRawValue();

    const request$ = this.mode() === 'register'
      ? this.authService.register(name, email, password)
      : this.authService.login(email, password);

    request$.subscribe({
      next: () => {
        this.loading.set(false);
        this.closed.emit();
      },
      error: (error: HttpErrorResponse) => {
        this.loading.set(false);
        this.error.set(this.extractError(error));
      }
    });
  }

  toggleMode(): void {
    this.setMode(this.mode() === 'login' ? 'register' : 'login');
  }

  close(): void {
    this.closed.emit();
  }

  showNameError(): boolean {
    const control = this.form.controls.name;
    return this.mode() === 'register'
      && control.invalid
      && (control.touched || this.submitted());
  }

  showEmailError(): boolean {
    const control = this.form.controls.email;
    return control.invalid && (control.touched || this.submitted());
  }

  showPasswordError(): boolean {
    const control = this.form.controls.password;
    return control.invalid && (control.touched || this.submitted());
  }

  private setMode(mode: AuthMode): void {

    this.mode.set(mode);
    this.error.set(null);
    this.submitted.set(false);

    const name = this.form.controls.name;

    if (mode === 'register') {
      name.setValidators([Validators.required]);
    } else {
      name.clearValidators();
    }

    name.updateValueAndValidity();
  }

  private extractError(error: HttpErrorResponse): string {

    const message = error?.error?.message;

    if (typeof message === 'string' && message.trim().length > 0) {
      return message;
    }

    if (error?.status === 0) {
      return 'No se ha podido conectar con el servidor.';
    }

    return 'Ha ocurrido un error. Inténtalo de nuevo.';
  }
}
