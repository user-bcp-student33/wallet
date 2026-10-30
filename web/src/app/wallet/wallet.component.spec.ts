import '@angular/compiler';
import { HttpErrorResponse } from '@angular/common/http';
import { Injector, runInInjectionContext } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { WalletApiService } from './wallet-api.service';
import { WalletComponent } from './wallet.component';
import { WalletSessionService } from './wallet-session.service';
import { CreatedAccount, LoginResponse, OnboardingState } from './wallet.models';

const accountId = 'c0ffee00-0000-4000-8000-000000000001';
const pending: OnboardingState = {
  accountId,
  status: 'PENDING_ONBOARDING',
  completedSteps: 1,
  totalSteps: 3,
  currentStep: 'WALLET_PREPARATION'
};
const ready: OnboardingState = { ...pending, completedSteps: 2, currentStep: 'WALLET_READY' };
const active: OnboardingState = { ...pending, status: 'ACTIVE', completedSteps: 3, currentStep: null };
const created: CreatedAccount = {
  accountId,
  card: { brand: 'VISA', lastFour: '1111' },
  onboarding: pending
};

function loginResponse(onboarding: OnboardingState): LoginResponse {
  return { accountId, phoneNumber: '987654321', status: onboarding.status, onboarding };
}

describe('WalletComponent', () => {
  let api: {
    register: ReturnType<typeof vi.fn>;
    login: ReturnType<typeof vi.fn>;
    completeStep: ReturnType<typeof vi.fn>;
  };
  let router: { navigate: ReturnType<typeof vi.fn> };
  let session: WalletSessionService;

  function createComponent(page?: string): WalletComponent {
    const injector = Injector.create({ providers: [
      { provide: WalletApiService, useValue: api },
      { provide: WalletSessionService, useValue: session },
      { provide: Router, useValue: router },
      { provide: ActivatedRoute, useValue: { snapshot: { data: page ? { page } : {} } } }
    ] });
    return runInInjectionContext(injector, () => new WalletComponent());
  }

  function fillValidRegistration(component: WalletComponent): void {
    component.cardNumber = '1234 5678 9012 3456';
    component.cvv = '123';
    component.phoneNumber = '987654321';
    component.password = 'abcdefgh';
  }

  beforeEach(() => {
    api = {
      register: vi.fn().mockReturnValue(of(created)),
      login: vi.fn().mockReturnValue(of(loginResponse(pending))),
      completeStep: vi.fn().mockReturnValue(of(pending))
    };
    router = { navigate: vi.fn().mockResolvedValue(true) };
    session = new WalletSessionService();
  });

  it('takes the page to display from the route and defaults to home', () => {
    expect(createComponent().page).toBe('home');
    expect(createComponent('contacts').page).toBe('contacts');
  });

  it('derives the visible progress label from each onboarding page', () => {
    expect(createComponent('approved').currentStepNumber).toBe(3);
    expect(createComponent('approved').currentStepLabel).toBe('Cuenta lista');
    expect(createComponent('register').currentStepNumber).toBe(1);
    expect(createComponent('register').currentStepLabel).toBe('Configura tu cuenta');
    expect(createComponent('loading').currentStepNumber).toBe(2);
    expect(createComponent('loading').currentStepLabel).toBe('Preparando tu billetera');
  });

  it('formats card, CVV and phone input and clears corrected field errors', () => {
    const component = createComponent('register');
    component.fieldErrors = { cardNumber: 'invalid', cvv: 'invalid', phoneNumber: 'invalid' };
    component.formatCard({ target: { value: '4111-1111 1111 1111' } } as unknown as Event);
    component.formatDigits({ target: { value: '1a23' } } as unknown as Event, 'cvv', 3);
    component.formatDigits({ target: { value: '987 654 3210' } } as unknown as Event, 'phoneNumber', 9);

    expect(component.cardNumber).toBe('4111 1111 1111 1111');
    expect(component.cvv).toBe('123');
    expect(component.phoneNumber).toBe('987654321');
    expect(component.fieldErrors).toEqual({ cardNumber: undefined, cvv: undefined, phoneNumber: undefined });
  });

  it('blocks invalid registrations before calling the API', () => {
    const component = createComponent('register');
    component.register();

    expect(api.register).not.toHaveBeenCalled();
    expect(router.navigate).not.toHaveBeenCalled();
    expect(component.fieldErrors).toMatchObject({
      cardNumber: expect.any(String), cvv: expect.any(String),
      phoneNumber: expect.any(String), password: expect.any(String)
    });
  });

  it('registers with digits only and the default channel, clears secrets and opens step 2', () => {
    const component = createComponent('register');
    fillValidRegistration(component);

    component.register();

    expect(api.register).toHaveBeenCalledWith({
      cardNumber: '1234567890123456', cvv: '123', phoneNumber: '987654321',
      password: 'abcdefgh', acquisitionChannel: 'direct'
    });
    expect(component.cardNumber).toBe('');
    expect(component.cvv).toBe('');
    expect(component.password).toBe('');
    expect(component.session.account()?.card?.lastFour).toBe('1111');
    expect(component.busy).toBe(false);
    expect(router.navigate).toHaveBeenCalledWith(['/', 'preparacion']);
  });

  it('shows that the phone is in use under the phone field and stays on registration', () => {
    api.register.mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 409,
      error: { code: 'ACCOUNT_CONFLICT', message: 'Conflict', fieldErrors: [] }
    })));
    const component = createComponent('register');
    fillValidRegistration(component);

    component.register();

    expect(component.fieldErrors.phoneNumber).toBe('Este número de celular ya está en uso.');
    expect(component.pageError).toBe('');
    expect(component.busy).toBe(false);
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('blocks login with an invalid phone or missing password', () => {
    const component = createComponent('login');
    component.login();

    expect(api.login).not.toHaveBeenCalled();
    expect(component.fieldErrors.phoneNumber).toBeDefined();
    expect(component.fieldErrors.password).toBe('Ingresa tu contraseña.');
  });

  it('opens contacts for an active account and clears the password', () => {
    api.login.mockReturnValue(of(loginResponse(active)));
    const component = createComponent('login');
    component.loginPhone = '987654321';
    component.loginPassword = 'abcdefgh';

    component.login();

    expect(api.login).toHaveBeenCalledWith('987654321', 'abcdefgh');
    expect(component.loginPassword).toBe('');
    expect(session.status()).toBe('ACTIVE');
    expect(router.navigate).toHaveBeenCalledWith(['/', 'contactos']);
  });

  it.each([
    [pending, 'preparacion'],
    [ready, 'cuenta-lista']
  ])('resumes a pending account at its current onboarding page', (onboarding, path) => {
    api.login.mockReturnValue(of(loginResponse(onboarding)));
    const component = createComponent('login');
    component.loginPhone = '987654321';
    component.loginPassword = 'abcdefgh';

    component.login();

    expect(router.navigate).toHaveBeenCalledWith(['/', path]);
  });

  it('shows a uniform message for invalid login credentials', () => {
    api.login.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 401 })));
    const component = createComponent('login');
    component.loginPhone = '987654321';
    component.loginPassword = 'incorrecta';

    component.login();

    expect(component.pageError).toBe('Celular o contraseña incorrectos.');
    expect(component.loginPassword).toBe('');
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('does nothing when onboarding cannot advance', () => {
    const component = createComponent('loading');
    component.continueOnboarding();
    expect(api.completeStep).not.toHaveBeenCalled();

    session.setAccount({ accountId, phoneNumber: '987654321', status: 'ACTIVE', onboarding: active });
    component.continueOnboarding();
    expect(api.completeStep).not.toHaveBeenCalled();

    session.setAccount({ accountId, phoneNumber: '987654321', status: 'PENDING_ONBOARDING', onboarding: pending });
    component.busy = true;
    component.continueOnboarding();
    expect(api.completeStep).not.toHaveBeenCalled();
  });

  it('moves from preparation to ready to contacts as each step completes', () => {
    session.setAccount({ accountId, phoneNumber: '987654321', status: 'PENDING_ONBOARDING', onboarding: pending });
    const component = createComponent('loading');
    api.completeStep.mockReturnValueOnce(of(ready));
    component.continueOnboarding();
    expect(api.completeStep).toHaveBeenNthCalledWith(1, accountId, 'WALLET_PREPARATION');
    expect(router.navigate).toHaveBeenLastCalledWith(['/', 'cuenta-lista']);

    api.completeStep.mockReturnValueOnce(of(active));
    component.continueOnboarding();
    expect(api.completeStep).toHaveBeenNthCalledWith(2, accountId, 'WALLET_READY');
    expect(router.navigate).toHaveBeenLastCalledWith(['/', 'contactos']);
    expect(session.status()).toBe('ACTIVE');
  });

  it('keeps the current page and shows the error when a step cannot be completed', () => {
    session.setAccount({ accountId, phoneNumber: '987654321', status: 'PENDING_ONBOARDING', onboarding: pending });
    api.completeStep.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 0 })));
    const component = createComponent('loading');

    component.continueOnboarding();

    expect(component.pageError).toContain('No pudimos conectar');
    expect(component.busy).toBe(false);
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('shows field-level messages returned by the backend', () => {
    api.register.mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 400,
      error: { code: 'VALIDATION_ERROR', message: 'Revisa los datos.', fieldErrors: [
        { field: 'phoneNumber', message: 'Celular inválido.' }
      ] }
    })));
    const component = createComponent('register');
    fillValidRegistration(component);

    component.register();

    expect(component.fieldErrors.phoneNumber).toBe('Celular inválido.');
    expect(component.pageError).toBe('Revisa los datos.');
  });

  it('filters contacts by name or phone', () => {
    const component = createComponent('contacts');
    component.query = 'ana';
    expect(component.filteredContacts.map((contact) => contact.name)).toEqual(['Ana Torres']);
    component.query = '955';
    expect(component.filteredContacts.map((contact) => contact.name)).toEqual(['Lucía Quispe']);
    component.query = 'nada';
    expect(component.filteredContacts).toEqual([]);
  });

  it('clears the in-memory session and returns home on logout', () => {
    session.setAccount({ accountId, phoneNumber: '987654321', status: 'ACTIVE', onboarding: active });
    const component = createComponent('contacts');

    component.logout();

    expect(session.account()).toBeNull();
    expect(router.navigate).toHaveBeenCalledWith(['/']);
  });
});
