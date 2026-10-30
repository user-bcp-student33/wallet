import '@angular/compiler';
import { HttpClient } from '@angular/common/http';
import { inject, Injector, runInInjectionContext } from '@angular/core';
import { describe, expect, it, vi } from 'vitest';
import { WALLET_API_URL, WalletApiService } from './wallet-api.service';

describe('WalletApiService', () => {
  function createApi() {
    const http = { post: vi.fn().mockReturnValue({ subscribe: vi.fn() }) };
    const injector = Injector.create({ providers: [
      WalletApiService,
      { provide: HttpClient, useValue: http },
      { provide: WALLET_API_URL, useValue: 'http://wallet.test' }
    ] });
    const api = runInInjectionContext(injector, () => inject(WalletApiService));
    return { api, http };
  }

  it('sends registration fields to the committed account endpoint', () => {
    const { api, http } = createApi();
    const request = {
      cardNumber: '4111111111111111',
      cvv: '123',
      phoneNumber: '987654321',
      password: 'abcdefgh',
      acquisitionChannel: 'direct'
    };

    api.register(request);

    expect(http.post).toHaveBeenCalledWith('http://wallet.test/accounts', request);
  });

  it('uses the existing login route and contract', () => {
    const { api, http } = createApi();

    api.login('987654321', 'abcdefgh');

    expect(http.post).toHaveBeenCalledWith('http://wallet.test/auth/login', {
      phoneNumber: '987654321', password: 'abcdefgh'
    });
  });

  it('completes the currently requested onboarding step', () => {
    const { api, http } = createApi();

    api.completeStep('account-id', 'WALLET_PREPARATION');

    expect(http.post).toHaveBeenCalledWith(
      'http://wallet.test/accounts/account-id/onboarding/steps/WALLET_PREPARATION/complete', {}
    );
  });
});
