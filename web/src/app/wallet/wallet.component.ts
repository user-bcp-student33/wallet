import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import {
  digitsOnly,
  FieldErrors,
  formatCardNumber,
  hasErrors,
  validatePhone,
  validateRegistration
} from './wallet-validation';
import { WalletApiService } from './wallet-api.service';
import { pathForOnboarding, WALLET_PATHS } from './wallet-navigation';
import { WalletSessionService } from './wallet-session.service';
import { ApiErrorResponse, OnboardingState, RegistrationRequest } from './wallet.models';

type WalletPage = 'home' | 'register' | 'login' | 'loading' | 'approved' | 'contacts';

const DEFAULT_ACQUISITION_CHANNEL = 'direct';
const PHONE_IN_USE_MESSAGE = 'Este número de celular ya está en uso.';

interface Contact {
  name: string;
  phone: string;
  initials: string;
}

@Component({
  selector: 'app-wallet',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './wallet.component.html'
})
export class WalletComponent {
  private readonly api = inject(WalletApiService);
  private readonly router = inject(Router);
  readonly session = inject(WalletSessionService);
  readonly paths = WALLET_PATHS;
  readonly page: WalletPage = inject(ActivatedRoute).snapshot.data['page'] ?? 'home';
  readonly contacts: Contact[] = [
    { name: 'Ana Torres', phone: '987 654 321', initials: 'AT' },
    { name: 'Carlos Mendoza', phone: '912 345 678', initials: 'CM' },
    { name: 'Lucía Quispe', phone: '955 443 322', initials: 'LQ' },
    { name: 'Miguel Rojas', phone: '933 221 144', initials: 'MR' }
  ];
  get filteredContacts(): Contact[] {
    const query = this.query.trim().toLocaleLowerCase('es');
    return this.contacts.filter((contact) =>
      contact.name.toLocaleLowerCase('es').includes(query) || contact.phone.replace(/\s/g, '').includes(query.replace(/\s/g, ''))
    );
  }

  cardNumber = '';
  cvv = '';
  phoneNumber = '';
  password = '';
  loginPhone = '';
  loginPassword = '';
  query = '';
  showPassword = false;
  busy = false;
  pageError = '';
  fieldErrors: FieldErrors = {};

  get currentStepNumber(): number {
    return this.page === 'register' ? 1 : this.page === 'loading' ? 2 : 3;
  }

  get currentStepLabel(): string {
    return this.page === 'register' ? 'Configura tu cuenta'
      : this.page === 'loading' ? 'Preparando tu billetera' : 'Cuenta lista';
  }

  formatCard(event: Event): void {
    this.cardNumber = formatCardNumber((event.target as HTMLInputElement).value);
    this.clearFieldError('cardNumber');
  }

  formatDigits(event: Event, field: 'cvv' | 'phoneNumber' | 'loginPhone', maximumLength: number): void {
    this[field] = digitsOnly((event.target as HTMLInputElement).value, maximumLength);
    this.clearFieldError(field === 'loginPhone' ? 'phoneNumber' : field);
  }

  register(): void {
    this.fieldErrors = validateRegistration(
      this.cardNumber.replace(/\s/g, ''), this.cvv, this.phoneNumber, this.password
    );
    if (hasErrors(this.fieldErrors)) return;

    const request: RegistrationRequest = {
      cardNumber: this.cardNumber.replace(/\s/g, ''),
      cvv: this.cvv,
      phoneNumber: this.phoneNumber,
      password: this.password,
      acquisitionChannel: DEFAULT_ACQUISITION_CHANNEL
    };
    this.clearRegistrationSecrets();
    this.busy = true;
    this.pageError = '';
    this.api.register(request).subscribe({
      next: (created) => {
        this.session.setAccount({
          accountId: created.accountId,
          phoneNumber: request.phoneNumber,
          status: created.onboarding.status,
          onboarding: created.onboarding,
          card: created.card
        });
        this.busy = false;
        this.goToOnboardingPage(created.onboarding);
      },
      error: (error: HttpErrorResponse) => {
        this.showApiError(error);
        this.busy = false;
      }
    });
  }

  login(): void {
    this.fieldErrors = {
      phoneNumber: validatePhone(this.loginPhone),
      password: this.loginPassword ? undefined : 'Ingresa tu contraseña.'
    };
    if (hasErrors(this.fieldErrors)) return;

    const phone = this.loginPhone;
    const password = this.loginPassword;
    this.loginPassword = '';
    this.busy = true;
    this.pageError = '';
    this.api.login(phone, password).subscribe({
      next: (result) => {
        this.session.setAccount({
          accountId: result.accountId,
          phoneNumber: result.phoneNumber,
          status: result.status,
          onboarding: result.onboarding
        });
        this.busy = false;
        this.goToOnboardingPage(result.onboarding);
      },
      error: (error: HttpErrorResponse) => {
        this.showApiError(error);
        this.busy = false;
      }
    });
  }

  continueOnboarding(): void {
    const account = this.session.account();
    const step = account?.onboarding.currentStep;
    if (!account || !step || this.busy) return;
    this.busy = true;
    this.pageError = '';
    this.api.completeStep(account.accountId, step).subscribe({
      next: (onboarding) => {
        this.session.updateOnboarding(onboarding);
        this.busy = false;
        this.goToOnboardingPage(onboarding);
      },
      error: (error: HttpErrorResponse) => {
        this.showApiError(error);
        this.busy = false;
      }
    });
  }

  logout(): void {
    this.session.clear();
    void this.router.navigate(['/']);
  }

  private goToOnboardingPage(onboarding: OnboardingState): void {
    void this.router.navigate(['/', pathForOnboarding(onboarding)]);
  }

  private clearRegistrationSecrets(): void {
    this.cardNumber = '';
    this.cvv = '';
    this.password = '';
    this.fieldErrors = {};
  }

  private clearFieldError(field: keyof FieldErrors): void {
    if (this.fieldErrors[field]) this.fieldErrors = { ...this.fieldErrors, [field]: undefined };
  }

  private showApiError(error: HttpErrorResponse): void {
    const body = error.error as ApiErrorResponse | undefined;
    if (body?.code === 'ACCOUNT_CONFLICT') {
      this.fieldErrors = { phoneNumber: PHONE_IN_USE_MESSAGE };
      return;
    }
    this.pageError = body?.message || 'No pudimos conectar con Wallet Perú. Inténtalo nuevamente.';
    if (body?.fieldErrors?.length) {
      this.fieldErrors = Object.fromEntries(body.fieldErrors.map(({ field, message }) => [field, message]));
    }
    if (error.status === 401) this.pageError = 'Celular o contraseña incorrectos.';
  }
}
