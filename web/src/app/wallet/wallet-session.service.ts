import { Injectable, signal } from '@angular/core';
import { AccountStatus, OnboardingState, WalletAccount } from './wallet.models';

@Injectable({ providedIn: 'root' })
export class WalletSessionService {
  readonly account = signal<WalletAccount | null>(null);

  setAccount(account: WalletAccount): void {
    this.account.set(account);
  }

  updateOnboarding(onboarding: OnboardingState): void {
    const current = this.account();
    if (current) this.account.set({ ...current, onboarding, status: onboarding.status });
  }

  status(): AccountStatus | null {
    return this.account()?.status ?? null;
  }

  clear(): void {
    this.account.set(null);
  }
}
