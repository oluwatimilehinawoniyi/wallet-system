package com.wallet.auth.api;

import java.util.UUID;

public interface UserAccountFacade {

    UUID requireUserIdByEmail(String email);
}
