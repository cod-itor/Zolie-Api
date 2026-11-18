package com.example.zolieapi.dto.response;

import java.util.List;

public record DatingPreferencesResponse(
        List<String> genders,
        int minAge,
        int maxAge,
        int maxDistanceKm
) {
}
