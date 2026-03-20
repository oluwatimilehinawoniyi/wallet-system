package com.wallet.auth.service;

import com.wallet.auth.api.UserAccountFacade;
import com.wallet.auth.model.User;
import com.wallet.auth.repository.UserRepository;
import com.wallet.common.exception.BadRequestException;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class UserAccountFacadeImpl implements UserAccountFacade {

    private final UserRepository userRepository;

    @Override
    public UUID requireUserIdByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new BadRequestException(
                        "Authenticated user not found"));
    }
}
