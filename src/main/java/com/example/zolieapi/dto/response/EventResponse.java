package com.example.zolieapi.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EventResponse(
        UUID id,
        String title,
        String description,
        String category,
        String coverUrl,
        Instant startsAt,
        Instant endsAt,
        PlaceResponse place,
        String venueName,
        boolean isFree,
        String priceNote,
        String visibility,
        boolean showGuestList,
        UserSummaryResponse host,
        int interestedCount,
        int goingCount,
        String myRsvp,
        List<UserSummaryResponse> friendsGoing,
        long friendsGoingCount,
        boolean isMine,
        Instant createdAt
) {
}
