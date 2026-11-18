package com.example.zolieapi.dto.response;

import java.util.List;
import java.util.UUID;

public record DatingCardResponse(
        UUID userId,
        String displayName,
        Integer age,
        String bio,
        String gender,
        String lookingFor,
        String education,
        String occupation,
        String city,
        String distanceBand,
        List<InterestResponse> interests,
        List<InterestResponse> sharedInterests,
        List<MediaItemResponse> photos,
        List<PromptAnswerResponse> promptAnswers
) {
}
