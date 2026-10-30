package com.bcp.wallet.backend.account;

import java.time.Clock;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bcp.wallet.backend.account.dto.AccountCreatedResponse;
import com.bcp.wallet.backend.account.dto.AccountCreatedResponse.LinkedCardResponse;
import com.bcp.wallet.backend.error.DuplicatePhoneException;
import com.bcp.wallet.backend.onboarding.OnboardingService;
import com.bcp.wallet.backend.onboarding.OnboardingStateResponse;
import com.bcp.wallet.backend.security.PasswordHasher;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accounts;
    private final LinkedCardRepository cards;
    private final PasswordHasher passwordHasher;
    private final OnboardingService onboarding;
    private final Clock clock;

    @Transactional
    public AccountCreatedResponse register(CreateAccountCommand command) {
        CardBrand brand = CardBrand.fromCardNumber(command.cardNumber());
        Account account = save(new Account(
                command.phoneNumber(),
                passwordHasher.hash(command.password()),
                Optional.ofNullable(command.acquisitionChannel()).orElse(AccountRules.DEFAULT_CHANNEL),
                clock.instant()));
        String lastFour = lastDigits(command.cardNumber());
        cards.save(new LinkedCard(account, brand, lastFour, clock.instant()));
        onboarding.begin(account);
        return new AccountCreatedResponse(account.getId(), new LinkedCardResponse(brand, lastFour),
                OnboardingStateResponse.from(account));
    }

    private Account save(Account account) {
        try {
            return accounts.saveAndFlush(account);
        } catch (DataIntegrityViolationException duplicatedPhone) {
            throw new DuplicatePhoneException();
        }
    }

    private static String lastDigits(String cardNumber) {
        return cardNumber.substring(Math.max(0, cardNumber.length() - AccountRules.CARD_LAST_DIGITS));
    }
}
