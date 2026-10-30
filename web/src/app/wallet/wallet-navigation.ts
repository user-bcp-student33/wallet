import { OnboardingState } from './wallet.models';

export const WALLET_PATHS = {
  register: 'registro',
  login: 'login',
  preparation: 'preparacion',
  ready: 'cuenta-lista',
  contacts: 'contactos'
} as const;

export function pathForOnboarding(onboarding: OnboardingState): string {
  if (onboarding.status === 'ACTIVE') return WALLET_PATHS.contacts;
  return onboarding.currentStep === 'WALLET_READY' ? WALLET_PATHS.ready : WALLET_PATHS.preparation;
}
