package com.example.zolieapi.security;

import com.example.zolieapi.config.ZolieProperties;
import com.example.zolieapi.exception.ApiException;
import com.example.zolieapi.exception.ErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TokenVerifier {

    private static final String DEV_PREFIX = "dev:";
    private static final String DEV_ADMIN_SUFFIX = ":admin";
    private static final String DEV_EMAIL_DOMAIN = "@dev.zolie.local";

    private final ZolieProperties properties;
    private final ObjectProvider<FirebaseAuth> firebaseAuth;

    public VerifiedToken verify(String token) {
        if (properties.auth().devTokensEnabled() && token.startsWith(DEV_PREFIX)) {
            return verifyDevToken(token.substring(DEV_PREFIX.length()));
        }
        FirebaseAuth auth = firebaseAuth.getIfAvailable();
        if (auth == null) {
            throw new ApiException(ErrorCode.INVALID_TOKEN);
        }
        return verifyFirebaseToken(auth, token);
    }

    private VerifiedToken verifyDevToken(String body) {
        boolean admin = body.endsWith(DEV_ADMIN_SUFFIX);
        String name = admin ? body.substring(0, body.length() - DEV_ADMIN_SUFFIX.length()) : body;
        if (!name.matches("[a-z0-9._]{3,30}")) {
            throw new ApiException(ErrorCode.INVALID_TOKEN);
        }
        return new VerifiedToken("dev-" + name, name + DEV_EMAIL_DOMAIN, admin);
    }

    private VerifiedToken verifyFirebaseToken(FirebaseAuth auth, String token) {
        try {
            FirebaseToken decoded = auth.verifyIdToken(token);
            boolean admin = Boolean.TRUE.equals(decoded.getClaims().get("admin"));
            return new VerifiedToken(decoded.getUid(), decoded.getEmail(), admin);
        } catch (FirebaseAuthException | IllegalArgumentException e) {
            throw new ApiException(ErrorCode.INVALID_TOKEN);
        }
    }
}
