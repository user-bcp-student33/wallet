import { HttpClient } from '@angular/common/http';
import { Injectable, InjectionToken, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { CreatedAccount, LoginResponse, OnboardingState, OnboardingStep, RegistrationRequest } from './wallet.models';

export const WALLET_API_URL = new InjectionToken<string>('WALLET_API_URL', {
  providedIn: 'root',
  factory: () => 'http://localhost:8080'
});

@Injectable({ providedIn: 'root' })
export class WalletApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(WALLET_API_URL);

  register(request: RegistrationRequest): Observable<CreatedAccount> {
    return this.http.post<CreatedAccount>(`${this.baseUrl}/accounts`, request);
  }

  login(phoneNumber: string, password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.baseUrl}/auth/login`, { phoneNumber, password });
  }

  completeStep(accountId: string, step: OnboardingStep): Observable<OnboardingState> {
    return this.http.post<OnboardingState>(
      `${this.baseUrl}/accounts/${accountId}/onboarding/steps/${step}/complete`, {}
    );
  }
}
