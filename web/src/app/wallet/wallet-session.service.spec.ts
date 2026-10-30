import { describe, expect, it } from 'vitest';
import { OnboardingState } from './wallet.models';
import { WalletSessionService } from './wallet-session.service';

describe('WalletSessionService', () => {
  it('keeps only the in-memory account state', () => {
    const session = new WalletSessionService();
    const onboarding: OnboardingState = {
      accountId: 'account-id',
      status: 'PENDING_ONBOARDING',
      completedSteps: 1,
      totalSteps: 3,
      currentStep: 'WALLET_PREPARATION'
    };
    session.setAccount({ accountId: 'account-id', phoneNumber: '987654321', status: 'PENDING_ONBOARDING', onboarding });

    expect(session.status()).toBe('PENDING_ONBOARDING');
    session.updateOnboarding({ ...onboarding, status: 'ACTIVE', completedSteps: 3, currentStep: null });
    expect(session.status()).toBe('ACTIVE');
    expect(session.account()?.onboarding.completedSteps).toBe(3);

    session.clear();
    expect(session.account()).toBeNull();
  });
});
