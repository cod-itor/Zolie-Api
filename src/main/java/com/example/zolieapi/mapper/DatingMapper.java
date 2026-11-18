package com.example.zolieapi.mapper;

import com.example.zolieapi.common.Ages;
import com.example.zolieapi.common.Enums;
import com.example.zolieapi.dto.response.DatingCardResponse;
import com.example.zolieapi.dto.response.DatingProfileResponse;
import com.example.zolieapi.dto.response.InterestResponse;
import com.example.zolieapi.dto.response.MediaItemResponse;
import com.example.zolieapi.model.DatingProfile;
import com.example.zolieapi.model.User;
import com.example.zolieapi.repository.DatingProfileRepository;
import com.example.zolieapi.repository.DatingProfileRepository.InterestRow;
import com.example.zolieapi.repository.DatingProfileRepository.PhotoRow;
import com.example.zolieapi.repository.UserRepository;
import com.example.zolieapi.service.MediaService;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DatingMapper {

    private static final int[] BANDS_KM = {1, 5, 10, 25, 50, 100, 200};

    private final DatingProfileRepository datingProfileRepository;
    private final UserRepository userRepository;
    private final MediaService mediaService;

    public DatingProfileResponse toOwnProfile(DatingProfile profile, User user) {
        List<UUID> userIds = List.of(profile.getUserId());
        return new DatingProfileResponse(
                profile.getUserId(),
                Enums.value(profile.getStatus()),
                user.getDisplayName(),
                Ages.of(user.getDateOfBirth()),
                profile.getBio(),
                Enums.value(profile.getGender()),
                Enums.value(profile.getLookingFor()),
                profile.getEducation(),
                profile.getOccupation(),
                profile.getCity(),
                datingProfileRepository.hasLocation(profile.getUserId()),
                profile.isShowAge(),
                profile.isShowCity(),
                profile.isShowEducation(),
                profile.isShowOccupation(),
                profile.isShowDistance(),
                interestsByUser(userIds).getOrDefault(profile.getUserId(), List.of()),
                photosByUser(userIds).getOrDefault(profile.getUserId(), List.of()));
    }

    public Map<UUID, DatingCardResponse> toCards(Collection<UUID> userIds, Map<UUID, Double> distancesMeters) {
        List<UUID> ids = userIds.stream().distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<UUID, User> users = userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<UUID, DatingProfile> profiles = datingProfileRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(DatingProfile::getUserId, Function.identity()));
        Map<UUID, List<InterestResponse>> interests = interestsByUser(ids);
        Map<UUID, List<MediaItemResponse>> photos = photosByUser(ids);

        Map<UUID, DatingCardResponse> cards = new LinkedHashMap<>();
        for (UUID userId : ids) {
            DatingProfile profile = profiles.get(userId);
            User user = users.get(userId);
            if (profile == null || user == null) {
                continue;
            }
            cards.put(userId, new DatingCardResponse(
                    userId,
                    user.getDisplayName(),
                    profile.isShowAge() ? Ages.of(user.getDateOfBirth()) : null,
                    profile.getBio(),
                    Enums.value(profile.getGender()),
                    Enums.value(profile.getLookingFor()),
                    profile.isShowEducation() ? profile.getEducation() : null,
                    profile.isShowOccupation() ? profile.getOccupation() : null,
                    profile.isShowCity() ? profile.getCity() : null,
                    profile.isShowDistance() ? distanceBand(distancesMeters.get(userId)) : null,
                    interests.getOrDefault(userId, List.of()),
                    photos.getOrDefault(userId, List.of())));
        }
        return cards;
    }

    private String distanceBand(Double meters) {
        if (meters == null) {
            return null;
        }
        for (int band : BANDS_KM) {
            if (meters <= band * 1000.0) {
                return "within " + band + " km";
            }
        }
        return "within " + BANDS_KM[BANDS_KM.length - 1] + " km";
    }

    private Map<UUID, List<InterestResponse>> interestsByUser(Collection<UUID> userIds) {
        return datingProfileRepository.findInterests(userIds).stream()
                .collect(Collectors.groupingBy(InterestRow::getUserId, Collectors.mapping(
                        row -> new InterestResponse(row.getId(), row.getNameEn(), row.getNameKm()),
                        Collectors.toList())));
    }

    private Map<UUID, List<MediaItemResponse>> photosByUser(Collection<UUID> userIds) {
        List<PhotoRow> rows = datingProfileRepository.findPhotos(userIds);
        Map<UUID, MediaItemResponse> items = mediaService.items(rows.stream().map(PhotoRow::getMediaId).toList());
        return rows.stream()
                .filter(row -> items.get(row.getMediaId()) != null)
                .collect(Collectors.groupingBy(PhotoRow::getUserId, Collectors.mapping(
                        row -> items.get(row.getMediaId()), Collectors.toList())));
    }
}
