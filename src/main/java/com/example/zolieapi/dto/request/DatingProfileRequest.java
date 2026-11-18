package com.example.zolieapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record DatingProfileRequest(
        @Size(max = 500) String bio,
        @NotBlank @Schema(allowableValues = {"woman", "man", "nonbinary", "other"}) String gender,
        @Schema(allowableValues = {"relationship", "casual", "friends", "not_sure"}) String lookingFor,
        @Size(max = 100) String education,
        @Size(max = 100) String occupation,
        @Size(max = 80) String city,
        List<Integer> interestIds,
        Boolean showAge,
        Boolean showCity,
        Boolean showEducation,
        Boolean showOccupation,
        Boolean showDistance
) {
}
