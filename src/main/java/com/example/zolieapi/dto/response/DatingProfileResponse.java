package com.example.zolieapi.dto.response;

import java.util.List;
import java.util.UUID;

public record DatingProfileResponse(
        UUID userId,
        String status,
        String displayName,
        int age,
        String bio,
        String gender,
        String lookingFor,
        String education,
        String occupation,
        String city,
        boolean hasLocation,
        boolean showAge,
        boolean showCity,
        boolean showEducation,
        boolean showOccupation,
        boolean showDistance,
        List<InterestResponse> interests,
        List<MediaItemResponse> photos
) {
}
