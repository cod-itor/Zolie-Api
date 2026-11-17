package com.example.zolieapi.security;

import java.util.UUID;

public record AuthUser(String firebaseUid, String email, boolean admin, UUID userId, boolean suspended) {

    public boolean isSynced() {
        return userId != null;
    }
}
