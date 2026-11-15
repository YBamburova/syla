package com.syla.core.identity;

import org.springframework.security.core.Authentication;

public interface IdentityLifecycle {
    void deleteProviderAccount(Authentication authentication);
}
