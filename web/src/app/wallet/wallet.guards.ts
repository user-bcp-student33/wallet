import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { WalletSessionService } from './wallet-session.service';
import { OnboardingStep } from './wallet.models';

export const activeWalletGuard: CanActivateFn = () => {
  const session = inject(WalletSessionService);
  return session.status() === 'ACTIVE' || inject(Router).createUrlTree(['/']);
};

export function onboardingStepGuard(step: OnboardingStep): CanActivateFn {
  return () => {
    const session = inject(WalletSessionService);
    return session.account()?.onboarding.currentStep === step || inject(Router).createUrlTree(['/']);
  };
}
