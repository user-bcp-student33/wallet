import { Routes } from '@angular/router';
import { WALLET_PATHS } from './wallet/wallet-navigation';
import { activeWalletGuard, onboardingStepGuard } from './wallet/wallet.guards';

const loadWallet = () => import('./wallet/wallet.component').then((module) => module.WalletComponent);

export const routes: Routes = [
  { path: '', loadComponent: loadWallet, data: { page: 'home' } },
  { path: WALLET_PATHS.register, loadComponent: loadWallet, data: { page: 'register' } },
  { path: WALLET_PATHS.login, loadComponent: loadWallet, data: { page: 'login' } },
  {
    path: WALLET_PATHS.preparation,
    loadComponent: loadWallet,
    data: { page: 'loading' },
    canActivate: [onboardingStepGuard('WALLET_PREPARATION')]
  },
  {
    path: WALLET_PATHS.ready,
    loadComponent: loadWallet,
    data: { page: 'approved' },
    canActivate: [onboardingStepGuard('WALLET_READY')]
  },
  { path: WALLET_PATHS.contacts, loadComponent: loadWallet, data: { page: 'contacts' }, canActivate: [activeWalletGuard] },
  { path: '**', redirectTo: '' }
];
