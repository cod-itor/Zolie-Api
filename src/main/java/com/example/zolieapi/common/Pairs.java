package com.example.zolieapi.common;

import java.util.UUID;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Pairs {

    public UUID first(UUID one, UUID other) {
        return one.toString().compareTo(other.toString()) < 0 ? one : other;
    }

    public UUID second(UUID one, UUID other) {
        return one.toString().compareTo(other.toString()) < 0 ? other : one;
    }

    public UUID other(UUID firstId, UUID secondId, UUID me) {
        return firstId.equals(me) ? secondId : firstId;
    }
}
