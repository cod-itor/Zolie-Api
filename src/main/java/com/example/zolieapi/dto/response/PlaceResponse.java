package com.example.zolieapi.dto.response;

import java.util.UUID;

public record PlaceResponse(
        UUID id,
        String type,
        String nameEn,
        String nameKm,
        boolean popular
) {
}
