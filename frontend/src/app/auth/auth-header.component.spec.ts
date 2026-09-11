import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';

import { AuthService } from '../services/auth.service';
import { AuthHeaderComponent } from './auth-header.component';

describe('AuthHeaderComponent', () => {

  function setup(authenticated: boolean) {
    const logout = vi.fn();

    TestBed.configureTestingModule({
      imports: [AuthHeaderComponent],
      providers: [
        {
          provide: AuthService,
          useValue: {
            isAuthenticated: () => authenticated,
            user: () => authenticated
              ? { id: 1, name: 'Dani', email: 'dani@example.com' }
              : null,
            logout,
          },
        },
      ],
    });

    const fixture = TestBed.createComponent(AuthHeaderComponent);
    fixture.detectChanges();

    return { fixture, logout };
  }

  it('muestra "Iniciar sesión" sin sesión y emite login', () => {
    const { fixture } = setup(false);
    const element = fixture.nativeElement as HTMLElement;

    const button = element.querySelector<HTMLButtonElement>('.auth-button');
    expect(button).not.toBeNull();

    const loginSpy = vi.fn();
    fixture.componentInstance.login.subscribe(loginSpy);

    button!.click();

    expect(loginSpy).toHaveBeenCalled();
  });

  it('muestra el nombre del usuario cuando hay sesión', () => {
    const { fixture } = setup(true);
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('.user-name')?.textContent).toContain('Dani');
    expect(element.querySelector('.auth-button')).toBeNull();
  });

  it('abre el desplegable con el email y permite cerrar sesión', () => {
    const { fixture, logout } = setup(true);
    const element = fixture.nativeElement as HTMLElement;

    element.querySelector<HTMLButtonElement>('.user-button')!.click();
    fixture.detectChanges();

    expect(element.querySelector('.user-dropdown-email')?.textContent)
      .toContain('dani@example.com');

    element.querySelector<HTMLButtonElement>('.user-dropdown-logout')!.click();

    expect(logout).toHaveBeenCalled();
  });

  it('emite plans desde la opción "Mis planes"', () => {
    const { fixture } = setup(true);
    const element = fixture.nativeElement as HTMLElement;

    element.querySelector<HTMLButtonElement>('.user-button')!.click();
    fixture.detectChanges();

    const plansSpy = vi.fn();
    fixture.componentInstance.plans.subscribe(plansSpy);

    element.querySelector<HTMLButtonElement>('.user-dropdown-item')!.click();

    expect(plansSpy).toHaveBeenCalled();
  });
});
