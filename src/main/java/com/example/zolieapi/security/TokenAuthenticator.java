package com.example.zolieapi.security;

import com.example.zolieapi.model.User;
import com.example.zolieapi.model.enums.AccountStatus;
import com.example.zolieapi.model.enums.UserRole;
import com.example.zolieapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TokenAuthenticator {

    private final TokenVerifier tokenVerifier;
    private final UserRepository userRepository;

    public AuthUser authenticate(String token) {
        VerifiedToken verified = tokenVerifier.verify(token);
        User user = userRepository.findByFirebaseUid(verified.uid()).orElse(null);
        if (user == null) {
            return new AuthUser(verified.uid(), verified.email(), verified.admin(), null, false);
        }
        boolean admin = verified.admin() || user.getRole() == UserRole.ADMIN;
        boolean suspended = user.getStatus() == AccountStatus.SUSPENDED;
        return new AuthUser(verified.uid(), verified.email(), admin, user.getId(), suspended);
    }
}
