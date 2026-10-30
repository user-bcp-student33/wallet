package com.bcp.wallet.backend.account;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.bcp.wallet.backend.account.dto.AccountCreatedResponse;
import com.bcp.wallet.backend.account.dto.CreateAccountRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountCreatedResponse register(@Valid @RequestBody CreateAccountRequest request) {
        return accountService.register(new CreateAccountCommand(request.getCardNumber(), request.getPhoneNumber(),
                request.getPassword(), request.getAcquisitionChannel()));
    }
}
