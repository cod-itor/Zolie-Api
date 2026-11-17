package com.example.zolieapi.security;

import java.util.UUID;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Viewer {

    public static final UUID GUEST = new UUID(0L, 0L);

    public boolean isGuest(UUID viewerId) {
        return GUEST.equals(viewerId);
    }
}
