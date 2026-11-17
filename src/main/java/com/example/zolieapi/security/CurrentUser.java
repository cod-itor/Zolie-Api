package com.example.zolieapi.security;

import com.example.zolieapi.exception.ApiException;
import com.example.zolieapi.exception.ErrorCode;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

    public AuthUser principal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUser authUser)) {
            throw new ApiException(ErrorCode.AUTH_REQUIRED);
        }
        return authUser;
    }

    public UUID id() {
        AuthUser authUser = principal();
        if (authUser.suspended()) {
            throw new ApiException(ErrorCode.ACCOUNT_SUSPENDED);
        }
        return syncedId(authUser);
    }

    public UUID idAllowingSuspended() {
        return syncedId(principal());
    }

    public UUID idOrGuest() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthUser authUser && authUser.isSynced()) {
            return authUser.userId();
        }
        return Viewer.GUEST;
    }

    private UUID syncedId(AuthUser authUser) {
        if (!authUser.isSynced()) {
            throw new ApiException(ErrorCode.PROFILE_REQUIRED);
        }
        return authUser.userId();
    }
}
