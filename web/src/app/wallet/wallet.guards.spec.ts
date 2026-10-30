import '@angular/compiler';
import { Injector, runInInjectionContext } from '@angular/core';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { pathForOnboarding, WALLET_PATHS } from './wallet-navigation';
import { WalletSessionService } from './wallet-session.service';
import { activeWalletGuard, onboardingStepGuard } from './wallet.guards';
import { OnboardingState } from './wallet.models';

const pending: OnboardingState = {
  accountId: 'id', status: 'PENDING_ONBOARDING', completedSteps: 1, totalSteps: 3, currentStep: 'WALLET_PREPARATION'
};
const ready: OnboardingState = { ...pending, completedSteps: 2, currentStep: 'WALLET_READY' };
const active: OnboardingState = { ...pending, status: 'ACTIVE', completedSteps: 3, currentStep: null };

describe('wallet navigation', () => {
  it('maps the onboarding state to its page', () => {
    expect(pathForOnboarding(pending)).toBe(WALLET_PATHS.preparation);
    expect(pathForOnboarding(ready)).toBe(WALLET_PATHS.ready);
    expect(pathForOnboarding(active)).toBe(WALLET_PATHS.contacts);
  });
});

describe('wallet guards', () => {
  const homeTree = {} as UrlTree;
  let session: WalletSessionService;
  let injector: Injector;

  const run = (guard: ReturnType<typeof onboardingStepGuard>) =>
    runInInjectionContext(injector, () => guard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot));

  beforeEach(() => {
    session = new WalletSessionService();
    injector = Injector.create({ providers: [
      { provide: WalletSessionService, useValue: session },
      { provide: Router, useValue: { createUrlTree: vi.fn().mockReturnValue(homeTree) } }
    ] });
  });

  it('allows contacts only for an active wallet', () => {
    expect(run(activeWalletGuard)).toBe(homeTree);

    session.setAccount({ accountId: 'id', phoneNumber: '987654321', status: 'PENDING_ONBOARDING', onboarding: pending });
    expect(run(activeWalletGuard)).toBe(homeTree);

    session.setAccount({ accountId: 'id', phoneNumber: '987654321', status: 'ACTIVE', onboarding: active });
    expect(run(activeWalletGuard)).toBe(true);
  });

  it('allows an onboarding page only while it is the current step', () => {
    expect(run(onboardingStepGuard('WALLET_PREPARATION'))).toBe(homeTree);

    session.setAccount({ accountId: 'id', phoneNumber: '987654321', status: 'PENDING_ONBOARDING', onboarding: pending });
    expect(run(onboardingStepGuard('WALLET_PREPARATION'))).toBe(true);
    expect(run(onboardingStepGuard('WALLET_READY'))).toBe(homeTree);

    session.updateOnboarding(ready);
    expect(run(onboardingStepGuard('WALLET_READY'))).toBe(true);
    expect(run(onboardingStepGuard('WALLET_PREPARATION'))).toBe(homeTree);
  });
});
