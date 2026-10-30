export type AccountStatus = 'PENDING_ONBOARDING' | 'ACTIVE';

export type OnboardingStep = 'ACCOUNT_SETUP' | 'WALLET_PREPARATION' | 'WALLET_READY';

export interface OnboardingState {
  accountId: string;
  status: AccountStatus;
  completedSteps: number;
  totalSteps: number;
  currentStep: OnboardingStep | null;
}

export interface CreatedAccount {
  accountId: string;
  card: { brand: 'VISA' | 'MASTERCARD' | 'OTHER'; lastFour: string };
  onboarding: OnboardingState;
}

export interface LoginResponse {
  accountId: string;
  phoneNumber: string;
  status: AccountStatus;
  onboarding: OnboardingState;
}

export interface ApiErrorResponse {
  code: string;
  message: string;
  fieldErrors: Array<{ field: string; message: string }>;
}

export interface WalletAccount {
  accountId: string;
  phoneNumber: string;
  status: AccountStatus;
  onboarding: OnboardingState;
  card?: CreatedAccount['card'];
}

export interface RegistrationRequest {
  cardNumber: string;
  cvv: string;
  phoneNumber: string;
  password: string;
  acquisitionChannel: string;
}
