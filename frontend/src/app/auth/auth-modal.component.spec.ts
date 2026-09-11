import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { UserProfile } from '../models/user-profile';
import { AuthService } from '../services/auth.service';
import { AuthModalComponent } from './auth-modal.component';

describe('AuthModalComponent', () => {

  const profile: UserProfile = {
    id: 1,
    name: 'Dani',
    email: 'dani@example.com',
  };

  function setup(
    loginImpl?: () => Observable<UserProfile>,
    registerImpl?: () => Observable<UserProfile>
  ) {
    const login = vi.fn(loginImpl ?? (() => of(profile)));
    const register = vi.fn(registerImpl ?? (() => of(profile)));

    TestBed.configureTestingModule({
      imports: [AuthModalComponent],
      providers: [
        { provide: AuthService, useValue: { login, register } },
      ],
    });

    const fixture = TestBed.createComponent(AuthModalComponent);
    fixture.detectChanges();

    return { fixture, component: fixture.componentInstance, login, register };
  }

  it('muestra el modo "Iniciar sesión" por defecto', () => {
    const { fixture } = setup();
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('#auth-title')?.textContent).toContain('Iniciar sesión');
    expect(element.querySelector('#auth-name')).toBeNull();
  });

  it('permite alternar entre iniciar sesión y crear cuenta', () => {
    const { fixture } = setup();
    const element = fixture.nativeElement as HTMLElement;

    element.querySelector<HTMLButtonElement>('.auth-toggle')!.click();
    fixture.detectChanges();

    expect(element.querySelector('#auth-title')?.textContent).toContain('Crear cuenta');
    expect(element.querySelector('#auth-name')).not.toBeNull();
  });

  it('hace login y emite closed al completarse', () => {
    const { fixture, component, login } = setup();

    const closedSpy = vi.fn();
    component.closed.subscribe(closedSpy);

    component.form.setValue({
      name: '',
      email: 'dani@example.com',
      password: 'password123',
    });
    component.submit();

    expect(login).toHaveBeenCalledWith('dani@example.com', 'password123');
    expect(closedSpy).toHaveBeenCalled();
  });

  it('registra usando name, email y password', () => {
    const { component, register } = setup();

    component.toggleMode();
    component.form.setValue({
      name: 'Dani',
      email: 'dani@example.com',
      password: 'password123',
    });
    component.submit();

    expect(register).toHaveBeenCalledWith('Dani', 'dani@example.com', 'password123');
  });

  it('muestra el error del backend', () => {
    const { fixture, component } = setup(
      () => throwError(() => new HttpErrorResponse({
        status: 401,
        error: { message: 'Credenciales inválidas' },
      }))
    );

    component.form.setValue({
      name: '',
      email: 'dani@example.com',
      password: 'password123',
    });
    component.submit();
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('.auth-error-global')?.textContent)
      .toContain('Credenciales inválidas');
  });

  it('no envía el formulario si es inválido', () => {
    const { component, login } = setup();

    component.form.setValue({
      name: '',
      email: 'no-valido',
      password: 'short',
    });
    component.submit();

    expect(login).not.toHaveBeenCalled();
  });
});
